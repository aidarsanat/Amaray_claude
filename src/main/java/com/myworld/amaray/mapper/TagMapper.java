package com.myworld.amaray.mapper;

import com.myworld.amaray.dto.common.TagDto;
import com.myworld.amaray.dto.common.TagNode;
import com.myworld.amaray.model.Tag;
import com.myworld.amaray.service.TagService;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TagMapper {

    public TagDto toDto(Tag t) {
        return new TagDto(t.getId(), t.getName(), t.getCategory(),
                t.getParent() == null ? null : t.getParent().getId(),
                t.isFolder(), t.isSelectable(), TagService.effectiveColor(t), t.getSortOrder());
    }

    /** Строит дерево из плоского списка одной категории (список уже отсортирован по sortOrder). */
    public List<TagNode> toTree(List<Tag> all) {
        return build(null, TagService.childrenMap(all));
    }

    private List<TagNode> build(Long parentId, Map<Long, List<Tag>> kids) {
        List<TagNode> out = new ArrayList<>();
        for (Tag t : kids.getOrDefault(parentId, List.of())) {
            out.add(new TagNode(toDto(t), build(t.getId(), kids)));
        }
        return out;
    }
}
