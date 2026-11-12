package com.myworld.amaray.dto.doc;

/**
 * POST: title обязателен, parentId (папка), isFolder и sortOrder необязательны.
 * PUT: меняются только присланные title и content (content у папки игнорируется).
 */
public record DocRequest(String title, String content, Long parentId, Boolean isFolder, Integer sortOrder) {
}
