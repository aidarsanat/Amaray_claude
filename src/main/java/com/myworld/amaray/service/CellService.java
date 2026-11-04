package com.myworld.amaray.service;

import com.myworld.amaray.dto.common.CellDto;
import com.myworld.amaray.dto.common.PairDto;
import com.myworld.amaray.dto.common.RefDto;
import com.myworld.amaray.dto.common.UsageDto;
import com.myworld.amaray.exception.BadRequestException;
import com.myworld.amaray.exception.NotFoundException;
import com.myworld.amaray.model.*;
import com.myworld.amaray.repository.CellTextRepository;
import com.myworld.amaray.repository.LinkRepository;
import com.myworld.amaray.repository.RecordStore;
import com.myworld.amaray.repository.TagRepository;
import com.myworld.amaray.schema.ColumnKind;
import com.myworld.amaray.schema.ColumnSpec;
import com.myworld.amaray.schema.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** Запись ячеек (ссылки, пары, текст, зеркала) и всё, что связано с использованием записей. */
@Service
@Transactional
@RequiredArgsConstructor
public class CellService {

    private final LinkRepository links;
    private final CellTextRepository texts;
    private final TagRepository tags;
    private final RecordStore store;

    // ───────────────────────── Запись ячеек ─────────────────────────

    public void apply(RefType type, Long id, Map<String, CellDto> cells) {
        for (Map.Entry<String, CellDto> e : cells.entrySet()) {
            ColumnSpec spec = Schema.require(type, e.getKey());
            CellDto cell = e.getValue() == null ? CellDto.empty() : e.getValue();
            if (spec.kind() == ColumnKind.MIRROR) {
                applyMirror(type, id, spec, cell);
            } else {
                applyOwn(type, id, spec, cell);
            }
        }
    }

    private void applyOwn(RefType type, Long id, ColumnSpec spec, CellDto cell) {
        List<Link> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        switch (spec.kind()) {
            case REFS -> {
                for (RefDto r : cell.refs()) {
                    requireIdAndType(r, spec);
                    if (!spec.targets().contains(r.type())) {
                        throw new BadRequestException("Столбец '" + spec.field() + "' не принимает ссылки типа " + r.type());
                    }
                    requireRecord(r.type(), r.id());
                    if (r.type() == type && r.id().equals(id)) {
                        throw new BadRequestException("Запись не может ссылаться на саму себя");
                    }
                    if (!seen.add(r.type() + ":" + r.id())) continue;
                    Link l = Link.of(type, id, spec.field(), result.size());
                    l.setTargetType(r.type());
                    l.setTargetId(r.id());
                    result.add(l);
                }
            }
            case TAGS -> {
                for (RefDto r : cell.refs()) {
                    if (r == null || r.id() == null) throw new BadRequestException("У тега не указан id");
                    checkTag(spec, r.id());
                    // в упорядоченных столбцах (сила: A, A+, S-) повторы разрешены
                    if (!spec.ordered() && !seen.add("TAG:" + r.id())) continue;
                    Link l = Link.of(type, id, spec.field(), result.size());
                    l.setTargetType(RefType.TAG);
                    l.setTargetId(r.id());
                    result.add(l);
                }
            }
            case PAIRS -> {
                for (PairDto p : cell.pairs()) {
                    Long tagId = p == null || p.tag() == null ? null : p.tag().id();
                    RefDto ref = p == null ? null : p.ref();
                    if (tagId == null && (ref == null || ref.id() == null)) {
                        throw new BadRequestException("Пара в столбце '" + spec.field() + "' пуста");
                    }
                    Link l = Link.of(type, id, spec.field(), result.size());
                    if (tagId != null) {
                        checkTag(spec, tagId);
                        l.setTargetType(RefType.TAG);
                        l.setTargetId(tagId);
                    }
                    if (ref != null && ref.id() != null) {
                        requireIdAndType(ref, spec);
                        if (!spec.targets().contains(ref.type())) {
                            throw new BadRequestException("Столбец '" + spec.field() + "' не принимает ссылки типа " + ref.type());
                        }
                        requireRecord(ref.type(), ref.id());
                        if (ref.type() == type && ref.id().equals(id)) {
                            throw new BadRequestException("Запись не может ссылаться на саму себя");
                        }
                        l.setRefType(ref.type());
                        l.setRefId(ref.id());
                    }
                    result.add(l);
                }
            }
            default -> throw new IllegalStateException();
        }

        if (spec.single() && result.size() > 1) {
            throw new BadRequestException("В столбце '" + spec.field() + "' может быть только одно значение");
        }
        if (spec.field().equals("origin")) {
            checkNoCycle(type, id, result);
        }

        links.deleteByOwnerTypeAndOwnerIdAndCol(type, id, spec.field());
        links.saveAll(result);
        if (type == RefType.RACE && spec.field().equals("origin")) touchRaceDescendants(id);
        saveText(type, id, spec.field(), cell.text());
    }

    private void saveText(RefType type, Long id, String field, String raw) {
        String text = Fields.text(raw);
        Optional<CellText> existing = texts.findByOwnerTypeAndOwnerIdAndCol(type, id, field);
        if (text == null) {
            existing.ifPresent(texts::delete);
            return;
        }
        CellText t = existing.orElseGet(() -> {
            CellText n = new CellText();
            n.setOwnerType(type);
            n.setOwnerId(id);
            n.setCol(field);
            return n;
        });
        t.setContent(text);
        texts.save(t);
    }

    /** Зеркало: правка записывается хозяевам (например, принадлежность у Land и Settlement), хозяева получают PENDING. */
    private void applyMirror(RefType type, Long id, ColumnSpec spec, CellDto cell) {
        Set<String> desired = new LinkedHashSet<>();
        for (RefDto r : cell.refs()) {
            requireIdAndType(r, spec);
            if (!spec.targets().contains(r.type())) {
                throw new BadRequestException("Столбец '" + spec.field() + "' принимает только: " + spec.targets());
            }
            requireRecord(r.type(), r.id());
            desired.add(r.type() + ":" + r.id());
        }

        Set<String> current = new HashSet<>();
        for (Link l : links.findByTargetTypeAndTargetIdAndColAndOwnerTypeIn(type, id, spec.mirrorCol(), spec.targets())) {
            String key = l.getOwnerType() + ":" + l.getOwnerId();
            current.add(key);
            if (!desired.contains(key)) {
                links.delete(l);
                store.markPending(l.getOwnerType(), l.getOwnerId());
            }
        }
        for (String key : desired) {
            if (current.contains(key)) continue;
            String[] parts = key.split(":");
            RefType ownerType = RefType.valueOf(parts[0]);
            Long ownerId = Long.valueOf(parts[1]);
            int pos = links.findByOwnerTypeAndOwnerIdAndColOrderByPos(ownerType, ownerId, spec.mirrorCol()).size();
            Link l = Link.of(ownerType, ownerId, spec.mirrorCol(), pos);
            l.setTargetType(type);
            l.setTargetId(id);
            links.save(l);
            store.markPending(ownerType, ownerId);
        }
    }

    // ───────────────────────── Проверки ─────────────────────────

    private void requireIdAndType(RefDto r, ColumnSpec spec) {
        if (r == null || r.id() == null || r.type() == null) {
            throw new BadRequestException("У ссылки в столбце '" + spec.field() + "' должны быть id и type");
        }
    }

    private void requireRecord(RefType type, Long id) {
        if (!type.isRecord()) throw new BadRequestException("Ожидалась ссылка на запись, а не на тег");
        if (store.find(type, id) == null) {
            throw new NotFoundException("Запись " + type.path() + " с id " + id + " не найдена");
        }
    }

    private Tag checkTag(ColumnSpec spec, Long tagId) {
        Tag t = tags.findById(tagId).orElseThrow(() -> new NotFoundException("Тег с id " + tagId + " не найден"));
        if (t.getCategory() != spec.category()) {
            throw new BadRequestException("Тег '" + t.getName() + "' из справочника " + t.getCategory()
                    + ", а столбец '" + spec.field() + "' использует " + spec.category());
        }
        if (t.isFolder() && !t.isSelectable()) {
            throw new BadRequestException("Папка '" + t.getName() + "' не может быть значением");
        }
        return t;
    }

    /** Происхождение не должно замыкаться в круг (дроу → эльфы → дроу). */
    private void checkNoCycle(RefType type, Long id, List<Link> newLinks) {
        Deque<Long> stack = new ArrayDeque<>();
        for (Link l : newLinks) {
            Long p = parentOf(l, type);
            if (p != null) stack.push(p);
        }
        Set<Long> seen = new HashSet<>();
        while (!stack.isEmpty()) {
            Long cur = stack.pop();
            if (cur.equals(id)) throw new BadRequestException("Происхождение не может замыкаться в круг");
            if (!seen.add(cur)) continue;
            for (Link l : links.findByOwnerTypeAndOwnerIdAndColOrderByPos(type, cur, "origin")) {
                Long p = parentOf(l, type);
                if (p != null) stack.push(p);
            }
        }
    }

    private Long parentOf(Link l, RefType type) {
        if (l.getRefType() == type) return l.getRefId();
        if (l.getTargetType() == type) return l.getTargetId();
        return null;
    }

    // ───────────────────────── Использование и удаление ─────────────────────────

    /** Где запись (или тег) используется. Для предупреждения перед удалением. */
    @Transactional(readOnly = true)
    public List<UsageDto> usagesOf(RefType type, Long id) {
        List<Link> all = new ArrayList<>(links.findByTargetTypeAndTargetId(type, id));
        all.addAll(links.findByRefTypeAndRefId(type, id));
        List<UsageDto> out = new ArrayList<>();
        for (Link l : all) {
            BaseEntity owner = store.find(l.getOwnerType(), l.getOwnerId());
            if (owner == null) continue;
            out.add(new UsageDto(l.getOwnerType(), l.getOwnerId(), owner.displayName(), l.getCol()));
        }
        return out;
    }

    /** Перед удалением записи: убирает ссылки на неё, владельцы получают PENDING. Сами записи не удаляются. */
    public void purge(RefType type, Long id) {
        if (type == RefType.RACE) touchRaceDescendants(id);
        for (Link l : links.findByTargetTypeAndTargetId(type, id)) {
            store.markPending(l.getOwnerType(), l.getOwnerId());
            links.delete(l);
        }
        for (Link l : links.findByRefTypeAndRefId(type, id)) {
            store.markPending(l.getOwnerType(), l.getOwnerId());
            if (l.getTargetId() != null) { // пара «тег + ссылка»: остаётся тег
                l.setRefType(null);
                l.setRefId(null);
                links.save(l);
            } else {
                links.delete(l);
            }
        }
        links.deleteByOwnerTypeAndOwnerId(type, id);
        texts.deleteByOwnerTypeAndOwnerId(type, id);
    }

    /** Запись или тег переименован: везде, где он показан, данные в Sheets устарели. */
    public void touchReferencers(RefType type, Long id) {
        for (Link l : links.findByTargetTypeAndTargetId(type, id)) store.markPending(l.getOwnerType(), l.getOwnerId());
        for (Link l : links.findByRefTypeAndRefId(type, id)) store.markPending(l.getOwnerType(), l.getOwnerId());
        if (type == RefType.RACE) touchRaceDescendants(id);
        if (type == RefType.TAG) {
            for (Link l : links.findByTargetTypeAndTargetId(RefType.TAG, id)) {
                if (l.getOwnerType() == RefType.RACE && "origin".equals(l.getCol())) touchRaceDescendants(l.getOwnerId());
            }
        }
    }

    /** Происхождение расы собирается по цепочке: правка звена меняет ячейку у всех потомков. */
    public void touchRaceDescendants(Long raceId) {
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(raceId);
        Set<Long> seen = new HashSet<>();
        while (!stack.isEmpty()) {
            Long cur = stack.pop();
            if (!seen.add(cur)) continue;
            for (Link l : links.findByRefTypeAndRefId(RefType.RACE, cur)) {
                if (l.getOwnerType() == RefType.RACE && "origin".equals(l.getCol())) {
                    store.markPending(RefType.RACE, l.getOwnerId());
                    stack.push(l.getOwnerId());
                }
            }
        }
    }
}
