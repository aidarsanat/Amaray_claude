package com.myworld.amaray.dto.common;

/** parentId = null переносит в корень справочника. position = место среди соседей (с 0). */
public record TagMoveRequest(Long parentId, Integer position) {
}
