package com.myworld.amaray.service;

import com.myworld.amaray.dto.common.UsageDto;
import com.myworld.amaray.dto.record.RecordRequest;
import com.myworld.amaray.exception.BadRequestException;
import com.myworld.amaray.exception.ConflictException;
import com.myworld.amaray.model.*;
import com.myworld.amaray.repository.LinkRepository;
import com.myworld.amaray.repository.RecordStore;
import com.myworld.amaray.schema.ColumnSpec;
import com.myworld.amaray.schema.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** Бизнес-логика всех девяти таблиц. Работает с сущностями, DTO собирает маппер. */
@Service
@Transactional
@RequiredArgsConstructor
public class RecordService {

    private final RecordStore store;
    private final LinkRepository links;
    private final CellService cells;
    private final TagService tagService;

    // ───────────────────────── Чтение ─────────────────────────

    @Transactional(readOnly = true)
    public BaseEntity get(RefType type, Long id) {
        return store.require(type, id);
    }

    @Transactional(readOnly = true)
    public List<BaseEntity> pending(RefType type) {
        return store.all(type).stream().filter(e -> e.getSyncState() == SyncState.PENDING).toList();
    }

    @Transactional(readOnly = true)
    public List<UsageDto> usages(RefType type, Long id) {
        store.require(type, id);
        return cells.usagesOf(type, id);
    }

    /** Персонажи, у которых фракция указана в «Фракции». */
    @Transactional(readOnly = true)
    public List<BaseEntity> membersOfFaction(Long factionId) {
        store.require(RefType.FACTION, factionId);
        Set<Long> ids = new LinkedHashSet<>();
        for (Link l : links.findByTargetTypeAndTargetId(RefType.FACTION, factionId)) {
            if (l.getOwnerType() == RefType.CHARACTER && "affiliation".equals(l.getCol())) ids.add(l.getOwnerId());
        }
        return store.byIds(RefType.CHARACTER, ids);
    }

    // ───────────────────────── Список: фильтры, поиск, сортировка, страницы ─────────────────────────

    @Transactional(readOnly = true)
    public PageResult<BaseEntity> list(RefType type, ListQuery q) {
        List<BaseEntity> items = new ArrayList<>(store.all(type));

        Set<Long> allowed = null;
        for (Map.Entry<String, String> f : q.filters().entrySet()) {
            Set<Long> ids = ownersFor(type, f.getKey(), f.getValue());
            if (allowed == null) allowed = ids;
            else allowed.retainAll(ids);
        }
        if (allowed != null) {
            Set<Long> keep = allowed;
            items.removeIf(e -> !keep.contains(e.getId()));
        }

        if (q.search() != null && !q.search().isBlank()) {
            String needle = q.search().trim().toLowerCase();
            items.removeIf(e -> !matches(e, needle));
        }

        sort(type, items, q.sortBy(), q.desc());

        long total = items.size();
        if (q.size() != null) {
            int size = Math.max(1, q.size());
            long from = Math.min(total, (long) Math.max(0, q.page() == null ? 0 : q.page()) * size);
            long to = Math.min(total, from + size);
            items = new ArrayList<>(items.subList((int) from, (int) to));
        }
        return new PageResult<>(items, total);
    }

    private boolean matches(BaseEntity e, String needle) {
        if (e.searchText() != null && e.searchText().toLowerCase().contains(needle)) return true;
        for (Object v : e.toFields().values()) {
            if (v instanceof String s && s.toLowerCase().contains(needle)) return true;
        }
        return false;
    }

    /** Id записей, подходящих под один фильтр. Значение: "5" или "ТИП:5". */
    private Set<Long> ownersFor(RefType type, String field, String raw) {
        ColumnSpec spec = Schema.find(type, field).orElseThrow(() ->
                new BadRequestException("Неизвестный фильтр или столбец '" + field + "'"));
        RefType explicit = null;
        long id;
        try {
            if (raw.contains(":")) {
                String[] p = raw.split(":", 2);
                explicit = RefType.valueOf(p[0].trim().toUpperCase());
                id = Long.parseLong(p[1].trim());
            } else {
                id = Long.parseLong(raw);
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Неверное значение фильтра '" + field + "': " + raw);
        }

        switch (spec.kind()) {
            case TAGS: // выбранная папка захватывает и вложенные теги
                return new HashSet<>(links.ownersByTarget(type, field, RefType.TAG, tagService.descendantIds(id)));
            case REFS: {
                RefType target = explicit != null ? explicit : onlyTarget(spec, field);
                if (!spec.targets().contains(target)) throw new BadRequestException("Столбец '" + field + "' не ссылается на " + target);
                return new HashSet<>(links.ownersByTarget(type, field, target, List.of(id)));
            }
            case PAIRS: // по умолчанию ищем по тегу пары, ссылка внутри пары игнорируется
                if (explicit != null && explicit != RefType.TAG) {
                    return new HashSet<>(links.ownersByRef(type, field, explicit, id));
                }
                return new HashSet<>(links.ownersByTarget(type, field, RefType.TAG, tagService.descendantIds(id)));
            case MIRROR: {
                RefType owner = explicit != null ? explicit : onlyTarget(spec, field);
                Set<Long> out = new HashSet<>();
                for (Link l : links.findByOwnerTypeAndOwnerIdAndColOrderByPos(owner, id, spec.mirrorCol())) {
                    if (l.getTargetType() == type) out.add(l.getTargetId());
                }
                return out;
            }
            default:
                throw new IllegalStateException();
        }
    }

    private RefType onlyTarget(ColumnSpec spec, String field) {
        if (spec.targets().size() != 1) {
            throw new BadRequestException("Столбец '" + field + "' принимает несколько типов, укажите ТИП:id, например LAND:3");
        }
        return spec.targets().iterator().next();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void sort(RefType type, List<BaseEntity> items, String sortByParam, boolean desc) {
        final String sortBy = (sortByParam == null || sortByParam.isBlank()) ? "name" : sortByParam;
        if (items.isEmpty()) return;

        Map<Long, Object> keys = new HashMap<>();
        if (sortBy.equals("id")) {
            items.forEach(e -> keys.put(e.getId(), e.getId()));
        } else if (sortBy.equals("syncState")) {
            items.forEach(e -> keys.put(e.getId(), e.getSyncState().name()));
        } else if (sortBy.equals("name")) {
            items.forEach(e -> keys.put(e.getId(), e.displayName()));
        } else if (items.get(0).toFields().containsKey(sortBy)) {
            items.forEach(e -> {
                Object v = e.toFields().get(sortBy);
                if (v instanceof Comparable) keys.put(e.getId(), v);
            });
        } else {
            ColumnSpec spec = Schema.find(type, sortBy).orElseThrow(() ->
                    new BadRequestException("Нельзя сортировать по '" + sortBy + "'"));
            if (spec.kind() != com.myworld.amaray.schema.ColumnKind.TAGS) {
                throw new BadRequestException("Сортировка по столбцу '" + sortBy + "' не поддерживается");
            }
            // по порядку справочника (S+ ... E-), у упорядоченных столбцов берётся последнее значение (текущая сила)
            Map<Long, Integer> rank = tagService.ranks(spec.category());
            for (Link l : links.findByOwnerTypeAndColOrderByPos(type, sortBy)) {
                Integer r = l.getTargetType() == RefType.TAG ? rank.get(l.getTargetId()) : null;
                if (r == null) continue;
                if (spec.ordered()) keys.put(l.getOwnerId(), r);
                else keys.putIfAbsent(l.getOwnerId(), r);
            }
        }

        items.sort((x, y) -> {
            Object a = keys.get(x.getId());
            Object b = keys.get(y.getId());
            if (a == null && b == null) return 0;
            if (a == null) return 1; // пустые всегда в конце
            if (b == null) return -1;
            int r = (a instanceof String s && b instanceof String t)
                    ? s.compareToIgnoreCase(t)
                    : ((Comparable) a).compareTo(b);
            return desc ? -r : r;
        });
    }

    // ───────────────────────── Создание, обновление, удаление ─────────────────────────

    public BaseEntity create(RefType type, RecordRequest req) {
        return create(type, req, SyncState.PENDING);
    }

    public BaseEntity create(RefType type, RecordRequest req, SyncState state) {
        BaseEntity e = store.newInstance(type);
        e.applyFields(req.fields());
        requireName(e);
        checkUnique(type, e, null);
        e.setSyncState(state);
        store.save(e);
        cells.apply(type, e.getId(), req.cells());
        return e;
    }

    public BaseEntity update(RefType type, Long id, RecordRequest req) {
        BaseEntity e = store.require(type, id);
        String oldName = e.displayName();
        e.applyFields(req.fields());
        requireName(e);
        checkUnique(type, e, id);
        e.setSyncState(SyncState.PENDING);
        cells.apply(type, id, req.cells());
        if (!Objects.equals(oldName, e.displayName())) {
            cells.touchReferencers(type, id); // название изменилось, в чужих ячейках в Sheets оно устарело
        }
        return e;
    }

    /** Подтверждение уже получено на фронте через /usages. Удаляются только ссылки на запись. */
    public void delete(RefType type, Long id) {
        BaseEntity e = store.require(type, id);
        cells.purge(type, id);
        store.delete(e);
    }

    public BaseEntity markSynced(RefType type, Long id) {
        BaseEntity e = store.require(type, id);
        e.setSyncState(SyncState.SYNCED);
        return e;
    }

    // ───────────────────────── Проверки ─────────────────────────

    private void requireName(BaseEntity e) {
        String n = e.mainName();
        if (n == null || n.isBlank()) {
            throw new BadRequestException("Обязательно только имя или название, но оно не указано");
        }
    }

    private void checkUnique(RefType type, BaseEntity e, Long excludeId) {
        if (e instanceof Characters c) {
            Map<String, Object> full = new LinkedHashMap<>();
            full.put("firstName", c.getFirstName());
            full.put("lastName", c.getLastName()); // null проверяется как IS NULL
            if (store.exists(type, full, excludeId)) {
                throw new ConflictException("Персонаж с именем '" + c.displayName() + "' уже существует");
            }
        } else if (e instanceof NamedEntity n) {
            if (store.exists(type, Map.of("name", n.getName()), excludeId)) {
                throw new ConflictException("Запись с названием '" + n.getName() + "' уже существует");
            }
        }
    }
}
