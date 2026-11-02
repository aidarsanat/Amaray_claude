package com.myworld.amaray.dto.record;

import com.myworld.amaray.dto.common.BacklinkDto;
import com.myworld.amaray.dto.common.CellDto;
import com.myworld.amaray.model.RefType;
import com.myworld.amaray.model.SyncState;

import java.util.List;
import java.util.Map;

/**
 * Ответ для таблицы и страницы записи.
 * fields: простые поля. cells: ячейки со ссылками. extras: вычисляемое (currentPower, origin).
 * backlinks заполняются только на странице записи.
 */
public record RecordDto(Long id, RefType type, String name, SyncState syncState,
                        Map<String, Object> fields, Map<String, CellDto> cells,
                        Map<String, Object> extras, List<BacklinkDto> backlinks) {
}
