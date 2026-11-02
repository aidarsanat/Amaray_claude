package com.myworld.amaray.dto.common;

import java.util.List;

/** Ячейка: ссылки (или пары) плюс свободный текст. */
public record CellDto(List<RefDto> refs, List<PairDto> pairs, String text) {

    public CellDto {
        refs = refs == null ? List.of() : refs;
        pairs = pairs == null ? List.of() : pairs;
    }

    public static CellDto empty() {
        return new CellDto(List.of(), List.of(), null);
    }
}
