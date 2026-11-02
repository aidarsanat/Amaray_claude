package com.myworld.amaray.dto.common;

import com.myworld.amaray.model.TagCategory;

/** color здесь уже с учётом наследования от папки. */
public record TagDto(Long id, String name, TagCategory category, Long parentId,
                     boolean isFolder, boolean selectable, String color, int sortOrder) {
}
