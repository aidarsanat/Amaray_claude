package com.myworld.amaray.dto.common;

/** Пара «тег + необязательная ссылка». Для статуса фракции, размножения и происхождения расы. */
public record PairDto(RefDto tag, RefDto ref) {
}
