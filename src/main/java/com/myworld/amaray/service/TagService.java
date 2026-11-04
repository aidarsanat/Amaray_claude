package com.myworld.amaray.service;

import com.myworld.amaray.dto.common.*;
import com.myworld.amaray.exception.BadRequestException;
import com.myworld.amaray.exception.ConflictException;
import com.myworld.amaray.exception.NotFoundException;
import com.myworld.amaray.model.Link;
import com.myworld.amaray.model.RefType;
import com.myworld.amaray.model.Tag;
import com.myworld.amaray.model.TagCategory;
import com.myworld.amaray.repository.LinkRepository;
import com.myworld.amaray.repository.RecordStore;
import com.myworld.amaray.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Consumer;

@Service
@Transactional
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tags;
    private final LinkRepository links;
    private final RecordStore store;
    private final CellService cellService;

    // ───────────────────────── Чтение ─────────────────────────

    @Transactional(readOnly = true)
    public List<Tag> categoryTags(TagCategory category) {
        return tags.findByCategoryOrderBySortOrderAscIdAsc(category);
    }

    @Transactional(readOnly = true)
    public Tag get(Long id) {
        return tags.findById(id).orElseThrow(() -> new NotFoundException("Тег с id " + id + " не найден"));
    }

    @Transactional(readOnly = true)
    public List<UsageDto> usages(Long id) {
        get(id);
        return cellService.usagesOf(RefType.TAG, id);
    }

    /** Подсказки при наборе: только то, что можно выбрать как значение. */
    @Transactional(readOnly = true)
    public List<Tag> suggest(TagCategory category, String q) {
        String needle = q == null ? "" : q.trim().toLowerCase();
        return categoryTags(category).stream()
                .filter(t -> !t.isFolder() || t.isSelectable())
                .filter(t -> t.getName().toLowerCase().contains(needle))
                .limit(20)
                .toList();
    }

    /** Тег и все вложенные в него (для фильтра по папке). */
    @Transactional(readOnly = true)
    public Set<Long> descendantIds(Long id) {
        Tag root = get(id);
        Map<Long, List<Tag>> kids = childrenMap(categoryTags(root.getCategory()));
        Set<Long> out = new LinkedHashSet<>();
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(id);
        while (!stack.isEmpty()) {
            Long cur = stack.pop();
            if (!out.add(cur)) continue;
            for (Tag k : kids.getOrDefault(cur, List.of())) stack.push(k.getId());
        }
        return out;
    }

    /** Место каждого тега в порядке справочника (для своей сортировки: S+ ... E-). */
    @Transactional(readOnly = true)
    public Map<Long, Integer> ranks(TagCategory category) {
        Map<Long, List<Tag>> kids = childrenMap(categoryTags(category));
        Map<Long, Integer> out = new HashMap<>();
        int[] n = {0};
        walk(null, kids, t -> out.put(t.getId(), n[0]++));
        return out;
    }

    // ───────────────────────── Изменение ─────────────────────────

    public Tag create(TagRequest r) {
        String name = r.name() == null ? null : r.name().trim();
        if (name == null || name.isEmpty()) throw new BadRequestException("Название тега обязательно");
        if (r.category() == null) throw new BadRequestException("Не указан справочник (category)");

        Tag parent = r.parentId() == null ? null : requireFolderParent(r.parentId(), r.category());
        checkSiblingName(r.category(), parent == null ? null : parent.getId(), name, null);

        boolean folder = Boolean.TRUE.equals(r.isFolder());
        Tag t = new Tag();
        t.setName(name);
        t.setCategory(r.category());
        t.setParent(parent);
        t.setFolder(folder);
        t.setSelectable(!folder || Boolean.TRUE.equals(r.selectable()));
        t.setColor(blankToNull(r.color()));
        t.setSortOrder(r.sortOrder() != null ? r.sortOrder()
                : siblings(r.category(), parent == null ? null : parent.getId(), null).size());
        return tags.save(t);
    }

    public Tag update(Long id, TagRequest r) {
        Tag t = get(id);
        if (r.name() != null) {
            String name = r.name().trim();
            if (name.isEmpty()) throw new BadRequestException("Название тега не может быть пустым");
            if (!name.equals(t.getName())) {
                checkSiblingName(t.getCategory(), t.getParent() == null ? null : t.getParent().getId(), name, id);
                t.setName(name);
                cellService.touchReferencers(RefType.TAG, id);
            }
        }
        if (r.color() != null) t.setColor(blankToNull(r.color()));
        if (r.selectable() != null && t.isFolder()) t.setSelectable(r.selectable());
        return tags.save(t);
    }

    /** Перенос в другую папку и/или смена места среди соседей (перетаскивание). */
    public Tag move(Long id, TagMoveRequest r) {
        Tag t = get(id);
        Tag newParent = r.parentId() == null ? null : requireFolderParent(r.parentId(), t.getCategory());
        if (newParent != null && descendantIds(id).contains(newParent.getId())) {
            throw new BadRequestException("Нельзя перенести папку внутрь самой себя");
        }
        Long newParentId = newParent == null ? null : newParent.getId();
        checkSiblingName(t.getCategory(), newParentId, t.getName(), id);

        List<Tag> sibs = new ArrayList<>(siblings(t.getCategory(), newParentId, id));
        int idx = r.position() == null ? sibs.size() : Math.max(0, Math.min(r.position(), sibs.size()));
        sibs.add(idx, t);
        t.setParent(newParent);
        for (int i = 0; i < sibs.size(); i++) sibs.get(i).setSortOrder(i);
        tags.saveAll(sibs);
        return t;
    }

    /**
     * Удаление тега или папки. Дети папки поднимаются на уровень выше. Ссылки на папку переходят на её родителя,
     * если он может быть значением, иначе убираются. Ссылки на обычный тег убираются. Записи не удаляются.
     */
    public void delete(Long id) {
        Tag t = get(id);
        Tag parent = t.getParent();
        Long newTarget = (t.isFolder() && parent != null && parent.isSelectable()) ? parent.getId() : null;

        for (Link l : links.findByTargetTypeAndTargetId(RefType.TAG, id)) {
            store.markPending(l.getOwnerType(), l.getOwnerId());
            if (l.getOwnerType() == RefType.RACE && "origin".equals(l.getCol())) cellService.touchRaceDescendants(l.getOwnerId());
            boolean duplicate = newTarget != null && links
                    .findByOwnerTypeAndOwnerIdAndColOrderByPos(l.getOwnerType(), l.getOwnerId(), l.getCol()).stream()
                    .anyMatch(o -> o.getTargetType() == RefType.TAG && newTarget.equals(o.getTargetId()));
            if (newTarget != null && !duplicate) {
                l.setTargetId(newTarget);
                links.save(l);
            } else if (l.getRefId() != null) { // пара: остаётся ссылка, пропадает только тег
                l.setTargetType(null);
                l.setTargetId(null);
                links.save(l);
            } else {
                links.delete(l);
            }
        }

        Long parentId = parent == null ? null : parent.getId();
        int next = siblings(t.getCategory(), parentId, id).size();
        for (Tag child : tags.findByCategoryOrderBySortOrderAscIdAsc(t.getCategory())) {
            if (child.getParent() != null && child.getParent().getId().equals(id)) {
                child.setParent(parent);
                child.setSortOrder(next++);
                tags.save(child);
            }
        }
        tags.delete(t);
    }

    // ───────────────────────── Вспомогательное ─────────────────────────

    private Tag requireFolderParent(Long parentId, TagCategory category) {
        Tag p = get(parentId);
        if (!p.isFolder()) throw new BadRequestException("'" + p.getName() + "' не папка, в неё нельзя вкладывать");
        if (p.getCategory() != category) throw new BadRequestException("Папка из другого справочника");
        return p;
    }

    private List<Tag> siblings(TagCategory category, Long parentId, Long excludeId) {
        return tags.findByCategoryOrderBySortOrderAscIdAsc(category).stream()
                .filter(x -> Objects.equals(x.getParent() == null ? null : x.getParent().getId(), parentId))
                .filter(x -> !x.getId().equals(excludeId))
                .toList();
    }

    private void checkSiblingName(TagCategory category, Long parentId, String name, Long excludeId) {
        boolean taken = siblings(category, parentId, excludeId).stream()
                .anyMatch(x -> x.getName().equalsIgnoreCase(name));
        if (taken) throw new ConflictException("В этой папке уже есть '" + name + "'");
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    public static Map<Long, List<Tag>> childrenMap(List<Tag> all) {
        Map<Long, List<Tag>> kids = new HashMap<>(); // ключ null = корень
        for (Tag t : all) {
            kids.computeIfAbsent(t.getParent() == null ? null : t.getParent().getId(), k -> new ArrayList<>()).add(t);
        }
        return kids;
    }

    private static void walk(Long parentId, Map<Long, List<Tag>> kids, Consumer<Tag> visit) {
        for (Tag t : kids.getOrDefault(parentId, List.of())) {
            visit.accept(t);
            walk(t.getId(), kids, visit);
        }
    }

    /** Свой цвет или цвет ближайшего родителя. */
    public static String effectiveColor(Tag t) {
        Tag cur = t;
        int guard = 0;
        while (cur != null && guard++ < 50) {
            if (cur.getColor() != null && !cur.getColor().isBlank()) return cur.getColor();
            cur = cur.getParent();
        }
        return null;
    }
}
