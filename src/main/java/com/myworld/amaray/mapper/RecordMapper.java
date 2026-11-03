package com.myworld.amaray.mapper;

import com.myworld.amaray.dto.common.*;
import com.myworld.amaray.dto.record.RecordDto;
import com.myworld.amaray.model.*;
import com.myworld.amaray.repository.CellTextRepository;
import com.myworld.amaray.repository.LinkRepository;
import com.myworld.amaray.repository.RecordStore;
import com.myworld.amaray.repository.TagRepository;
import com.myworld.amaray.schema.ColumnKind;
import com.myworld.amaray.schema.ColumnSpec;
import com.myworld.amaray.schema.Schema;
import com.myworld.amaray.service.RaceOriginService;
import com.myworld.amaray.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Сущность → DTO. Склеивает связи (Link) и текст (CellText) в ячейки {refs, pairs, text}.
 * Обратное направление (запрос → сущность) делает RecordService: простые поля применяет сама сущность,
 * ячейки раскладывает CellService.
 */
@Component
@RequiredArgsConstructor
public class RecordMapper {

    private final LinkRepository links;
    private final CellTextRepository texts;
    private final TagRepository tags;
    private final RecordStore store;
    private final RaceOriginService raceOrigin;

    public RecordDto toDto(RefType type, BaseEntity e, boolean detailed) {
        return toDtos(type, List.of(e), detailed).get(0);
    }

    /** detailed = страница записи: добавляются backlinks и вычисляемые поля вроде происхождения расы. */
    public List<RecordDto> toDtos(RefType type, List<BaseEntity> entities, boolean detailed) {
        if (entities.isEmpty()) return List.of();
        List<Long> ids = entities.stream().map(BaseEntity::getId).toList();
        List<ColumnSpec> columns = Schema.columns(type);

        Map<Long, List<Link>> own = new HashMap<>();
        for (Link l : links.findByOwnerTypeAndOwnerIdInOrderByPos(type, ids)) {
            own.computeIfAbsent(l.getOwnerId(), k -> new ArrayList<>()).add(l);
        }
        Map<Long, Map<String, String>> text = new HashMap<>();
        for (CellText t : texts.findByOwnerTypeAndOwnerIdIn(type, ids)) {
            text.computeIfAbsent(t.getOwnerId(), k -> new HashMap<>()).put(t.getCol(), t.getContent());
        }
        // зеркала: поле -> id записи -> ссылки хозяев
        Map<String, Map<Long, List<Link>>> mirror = new HashMap<>();
        for (ColumnSpec c : columns) {
            if (c.kind() != ColumnKind.MIRROR) continue;
            Map<Long, List<Link>> byTarget = new HashMap<>();
            for (Link l : links.findByTargetTypeAndTargetIdInAndColAndOwnerTypeIn(type, ids, c.mirrorCol(), c.targets())) {
                byTarget.computeIfAbsent(l.getTargetId(), k -> new ArrayList<>()).add(l);
            }
            mirror.put(c.field(), byTarget);
        }

        Names names = new Names();
        own.values().forEach(ls -> ls.forEach(names::collect));
        mirror.values().forEach(m -> m.values().forEach(ls -> ls.forEach(l -> names.want(l.getOwnerType(), l.getOwnerId()))));
        names.load();

        List<RecordDto> out = new ArrayList<>();
        for (BaseEntity e : entities) {
            List<Link> mine = own.getOrDefault(e.getId(), List.of());
            Map<String, CellDto> cells = new LinkedHashMap<>();

            for (ColumnSpec c : columns) {
                String cellText = text.getOrDefault(e.getId(), Map.of()).get(c.field());
                switch (c.kind()) {
                    case MIRROR -> {
                        List<RefDto> refs = new ArrayList<>();
                        for (Link l : mirror.get(c.field()).getOrDefault(e.getId(), List.of())) {
                            RefDto r = names.get(l.getOwnerType(), l.getOwnerId());
                            if (r != null) refs.add(r);
                        }
                        cells.put(c.field(), new CellDto(refs, List.of(), null));
                    }
                    case REFS, TAGS -> {
                        List<RefDto> refs = new ArrayList<>();
                        for (Link l : mine) {
                            if (!l.getCol().equals(c.field())) continue;
                            RefDto r = names.get(l.getTargetType(), l.getTargetId());
                            if (r != null) refs.add(r);
                        }
                        cells.put(c.field(), new CellDto(refs, List.of(), cellText));
                    }
                    case PAIRS -> {
                        List<PairDto> pairs = new ArrayList<>();
                        for (Link l : mine) {
                            if (!l.getCol().equals(c.field())) continue;
                            pairs.add(new PairDto(names.get(l.getTargetType(), l.getTargetId()),
                                    names.get(l.getRefType(), l.getRefId())));
                        }
                        cells.put(c.field(), new CellDto(List.of(), pairs, cellText));
                    }
                }
            }

            Map<String, Object> extras = new LinkedHashMap<>();
            if (type == RefType.CHARACTER) { // текущая сила = последнее значение прогресса
                List<RefDto> power = cells.get("power").refs();
                extras.put("currentPower", power.isEmpty() ? null : power.get(power.size() - 1));
            }
            if (type == RefType.RACE) {
                extras.put("origin", raceOrigin.describe(e.getId()));
            }

            out.add(new RecordDto(e.getId(), type, e.displayName(), e.getSyncState(),
                    e.toFields(), cells, extras,
                    detailed ? backlinks(type, e.getId(), columns) : List.of()));
        }
        return out;
    }

    /** Кто ссылается на запись. Зеркальные столбцы не дублируем: они уже есть в cells. */
    private List<BacklinkDto> backlinks(RefType type, Long id, List<ColumnSpec> columns) {
        Set<String> hidden = new HashSet<>();
        for (ColumnSpec c : columns) {
            if (c.kind() == ColumnKind.MIRROR) c.targets().forEach(o -> hidden.add(o + ":" + c.mirrorCol()));
        }
        List<Link> all = new ArrayList<>(links.findByTargetTypeAndTargetId(type, id));
        all.addAll(links.findByRefTypeAndRefId(type, id));

        Map<String, List<RefDto>> grouped = new LinkedHashMap<>();
        Map<String, Link> sample = new HashMap<>();
        for (Link l : all) {
            String key = l.getOwnerType() + ":" + l.getCol();
            if (hidden.contains(key)) continue;
            BaseEntity owner = store.find(l.getOwnerType(), l.getOwnerId());
            if (owner == null) continue;
            grouped.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(new RefDto(owner.getId(), owner.displayName(), l.getOwnerType(), null));
            sample.putIfAbsent(key, l);
        }
        List<BacklinkDto> out = new ArrayList<>();
        grouped.forEach((key, refs) -> out.add(new BacklinkDto(sample.get(key).getOwnerType(), sample.get(key).getCol(), refs)));
        return out;
    }

    /** Подгружает имена и цвета всех упомянутых записей и тегов одним проходом. */
    private class Names {
        private final Map<RefType, Set<Long>> wanted = new EnumMap<>(RefType.class);
        private final Map<String, RefDto> loaded = new HashMap<>();

        void want(RefType t, Long id) {
            if (t != null && id != null) wanted.computeIfAbsent(t, k -> new HashSet<>()).add(id);
        }

        void collect(Link l) {
            want(l.getTargetType(), l.getTargetId());
            want(l.getRefType(), l.getRefId());
        }

        void load() {
            for (Map.Entry<RefType, Set<Long>> w : wanted.entrySet()) {
                RefType t = w.getKey();
                if (t == RefType.TAG) {
                    for (Tag tag : tags.findAllById(w.getValue())) {
                        loaded.put(key(t, tag.getId()),
                                new RefDto(tag.getId(), tag.getName(), t, TagService.effectiveColor(tag)));
                    }
                } else {
                    for (BaseEntity e : store.byIds(t, w.getValue())) {
                        loaded.put(key(t, e.getId()), new RefDto(e.getId(), e.displayName(), t, null));
                    }
                }
            }
        }

        RefDto get(RefType t, Long id) {
            return t == null || id == null ? null : loaded.get(key(t, id));
        }

        private String key(RefType t, Long id) {
            return t + ":" + id;
        }
    }
}
