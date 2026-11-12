package com.myworld.amaray.service;

import com.myworld.amaray.dto.doc.DocDto;
import com.myworld.amaray.dto.doc.DocMoveRequest;
import com.myworld.amaray.dto.doc.DocNode;
import com.myworld.amaray.dto.doc.DocRequest;
import com.myworld.amaray.exception.BadRequestException;
import com.myworld.amaray.exception.NotFoundException;
import com.myworld.amaray.model.Doc;
import com.myworld.amaray.repository.DocRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class DocService {

    private final DocRepository docs;

    // ───────────────────────── Чтение ─────────────────────────

    /** Всё дерево без текстов. */
    @Transactional(readOnly = true)
    public List<DocNode> tree() {
        Map<Long, List<Doc>> kids = new HashMap<>(); // ключ null = корень
        for (Doc d : docs.findAllByOrderBySortOrderAscIdAsc()) {
            kids.computeIfAbsent(parentId(d), k -> new ArrayList<>()).add(d);
        }
        return build(null, kids);
    }

    @Transactional(readOnly = true)
    public Doc get(Long id) {
        return docs.findById(id).orElseThrow(() -> new NotFoundException("Документ с id " + id + " не найден"));
    }

    // ───────────────────────── Изменение ─────────────────────────

    public Doc create(DocRequest r) {
        String title = r.title() == null ? "" : r.title().trim();
        if (title.isEmpty()) throw new BadRequestException("Название документа обязательно");

        Doc parent = r.parentId() == null ? null : requireFolder(r.parentId());
        boolean folder = Boolean.TRUE.equals(r.isFolder());

        Doc d = new Doc();
        d.setTitle(title);
        d.setParent(parent);
        d.setFolder(folder);
        d.setContent(folder ? null : r.content());
        d.setSortOrder(r.sortOrder() != null ? r.sortOrder()
                : siblings(parent == null ? null : parent.getId(), null).size());
        return docs.save(d);
    }

    public Doc update(Long id, DocRequest r) {
        Doc d = get(id);
        if (r.title() != null) {
            String title = r.title().trim();
            if (title.isEmpty()) throw new BadRequestException("Название документа не может быть пустым");
            d.setTitle(title);
        }
        if (r.content() != null && !d.isFolder()) d.setContent(r.content());
        return docs.save(d);
    }

    /** Перенос в другую папку и/или смена места среди соседей. */
    public Doc move(Long id, DocMoveRequest r) {
        Doc d = get(id);
        Doc newParent = r.parentId() == null ? null : requireFolder(r.parentId());
        if (newParent != null && descendantIds(id).contains(newParent.getId())) {
            throw new BadRequestException("Нельзя перенести папку внутрь самой себя");
        }
        Long newParentId = newParent == null ? null : newParent.getId();

        List<Doc> sibs = new ArrayList<>(siblings(newParentId, id));
        int idx = r.position() == null ? sibs.size() : Math.max(0, Math.min(r.position(), sibs.size()));
        sibs.add(idx, d);
        d.setParent(newParent);
        for (int i = 0; i < sibs.size(); i++) sibs.get(i).setSortOrder(i);
        docs.saveAll(sibs);
        return d;
    }

    /** Удаление документа или папки. Содержимое папки не удаляется, а поднимается на уровень выше. */
    public void delete(Long id) {
        Doc d = get(id);
        Doc parent = d.getParent();
        Long parentId = parent == null ? null : parent.getId();

        int next = siblings(parentId, id).size();
        for (Doc child : docs.findAllByOrderBySortOrderAscIdAsc()) {
            if (child.getParent() != null && child.getParent().getId().equals(id)) {
                child.setParent(parent);
                child.setSortOrder(next++);
                docs.save(child);
            }
        }
        docs.delete(d);
    }

    // ───────────────────────── Вспомогательное ─────────────────────────

    private Long parentId(Doc d) {
        return d.getParent() == null ? null : d.getParent().getId();
    }

    private List<Doc> siblings(Long parentId, Long excludeId) {
        return docs.findAllByOrderBySortOrderAscIdAsc().stream()
                .filter(x -> Objects.equals(parentId(x), parentId))
                .filter(x -> !x.getId().equals(excludeId))
                .toList();
    }

    private Doc requireFolder(Long id) {
        Doc p = get(id);
        if (!p.isFolder()) throw new BadRequestException("'" + p.getTitle() + "' не папка, внутрь неё нельзя класть");
        return p;
    }

    /** Документ и все вложенные в него (чтобы не перенести папку внутрь самой себя). */
    private Set<Long> descendantIds(Long id) {
        Map<Long, List<Doc>> kids = new HashMap<>();
        for (Doc d : docs.findAll()) kids.computeIfAbsent(parentId(d), k -> new ArrayList<>()).add(d);
        Set<Long> out = new LinkedHashSet<>();
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(id);
        while (!stack.isEmpty()) {
            Long cur = stack.pop();
            if (!out.add(cur)) continue;
            for (Doc k : kids.getOrDefault(cur, List.of())) stack.push(k.getId());
        }
        return out;
    }

    private List<DocNode> build(Long parentId, Map<Long, List<Doc>> kids) {
        List<DocNode> out = new ArrayList<>();
        for (Doc d : kids.getOrDefault(parentId, List.of())) {
            out.add(new DocNode(DocDto.of(d, false), build(d.getId(), kids)));
        }
        return out;
    }
}
