package com.myworld.amaray.dto.doc;

import com.myworld.amaray.model.Doc;

import java.time.Instant;

/** В дереве content = null (текст тяжёлый), полный текст приходит в GET /api/docs/{id}. */
public record DocDto(Long id, String title, String content, Long parentId, boolean isFolder,
                     int sortOrder, Instant createdAt, Instant updatedAt) {

    public static DocDto of(Doc d, boolean withContent) {
        return new DocDto(d.getId(), d.getTitle(), withContent ? d.getContent() : null,
                d.getParent() == null ? null : d.getParent().getId(), d.isFolder(),
                d.getSortOrder(), d.getCreatedAt(), d.getUpdatedAt());
    }
}
