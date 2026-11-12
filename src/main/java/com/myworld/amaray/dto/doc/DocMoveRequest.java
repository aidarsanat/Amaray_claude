package com.myworld.amaray.dto.doc;

/** parentId = null переносит в корень. position = место среди соседей (с 0). */
public record DocMoveRequest(Long parentId, Integer position) {
}
