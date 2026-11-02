package com.myworld.amaray.dto.common;

import com.myworld.amaray.model.RefType;

/** Короткая карточка записи или тега. При отправке с фронта достаточно id и type. */
public record RefDto(Long id, String name, RefType type, String color) {
}
