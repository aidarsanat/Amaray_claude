package com.myworld.amaray.dto.common;

import com.myworld.amaray.model.TagCategory;

/**
 * color: цвет для показа, уже с учётом наследования от папки.
 * ownColor: свой цвет тега; null значит «наследуется». Его фронт показывает в форме редактирования,
 * чтобы не отправить обратно унаследованный цвет и не «заморозить» его.
 */
public record TagDto(Long id, String name, TagCategory category, Long parentId,
                     boolean isFolder, boolean selectable, String color, String ownColor,
                     int sortOrder) {
}
