package com.myworld.amaray.dto.common;

import com.myworld.amaray.model.TagCategory;

/** Для POST все поля кроме name и category необязательны. Для PUT меняются только присланные name, color, selectable. */
public record TagRequest(String name, TagCategory category, Long parentId,
                         Boolean isFolder, Boolean selectable, String color, Integer sortOrder) {
}
