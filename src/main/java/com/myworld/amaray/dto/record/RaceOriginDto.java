package com.myworld.amaray.dto.record;

import com.myworld.amaray.dto.common.RefDto;

/** parent и cause у самой расы. formula: «Люди + Аспект + Проклятие». chain: «дроу → эльфы → люди». */
public record RaceOriginDto(RefDto parent, RefDto cause, String formula, String chain) {
}
