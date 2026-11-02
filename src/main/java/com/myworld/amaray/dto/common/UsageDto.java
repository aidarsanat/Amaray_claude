package com.myworld.amaray.dto.common;

import com.myworld.amaray.model.RefType;

/** Где используется запись или тег: в какой записи и в каком столбце. */
public record UsageDto(RefType entity, Long id, String name, String column) {
}
