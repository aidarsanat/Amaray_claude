package com.myworld.amaray.dto.common;

import com.myworld.amaray.model.RefType;

import java.util.List;

/** Обратная сторона связи: кто ссылается на эту запись (из какой таблицы и столбца). */
public record BacklinkDto(RefType type, String column, List<RefDto> refs) {
}
