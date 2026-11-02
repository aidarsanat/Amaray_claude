package com.myworld.amaray.dto.record;

import com.myworld.amaray.dto.common.CellDto;

import java.util.Map;

/**
 * Вход для POST и PUT. Нет id и syncState, подменить их нельзя.
 * Применяется только то, что прислано: отсутствующее поле или ячейка не меняются,
 * чтобы очистить ячейку, пришлите пустую ячейку.
 */
public record RecordRequest(Map<String, Object> fields, Map<String, CellDto> cells) {

    public RecordRequest {
        fields = fields == null ? Map.of() : fields;
        cells = cells == null ? Map.of() : cells;
    }
}
