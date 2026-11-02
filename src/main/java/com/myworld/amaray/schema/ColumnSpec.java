package com.myworld.amaray.schema;

import com.myworld.amaray.model.RefType;
import com.myworld.amaray.model.TagCategory;

import java.util.Arrays;
import java.util.Set;

/**
 * Описание столбца со связями.
 * targets: для REFS допустимые типы ссылок, для PAIRS допустимые типы ссылки в паре,
 * для MIRROR таблицы-хозяева, где лежат данные (в столбце mirrorCol).
 */
public record ColumnSpec(String field, ColumnKind kind, Set<RefType> targets, TagCategory category,
                         boolean single, boolean ordered, String mirrorCol) {

    /** Парное текстовое поле есть у всех столбцов, кроме зеркал. */
    public boolean hasText() {
        return kind != ColumnKind.MIRROR;
    }

    public static ColumnSpec refs(String field, boolean single, RefType... types) {
        return new ColumnSpec(field, ColumnKind.REFS, Set.copyOf(Arrays.asList(types)), null, single, false, null);
    }

    public static ColumnSpec tags(String field, TagCategory category, boolean single, boolean ordered) {
        return new ColumnSpec(field, ColumnKind.TAGS, Set.of(), category, single, ordered, null);
    }

    public static ColumnSpec pairs(String field, TagCategory category, boolean single, RefType... refTypes) {
        return new ColumnSpec(field, ColumnKind.PAIRS, Set.copyOf(Arrays.asList(refTypes)), category, single, false, null);
    }

    public static ColumnSpec mirror(String field, String mirrorCol, RefType... owners) {
        return new ColumnSpec(field, ColumnKind.MIRROR, Set.copyOf(Arrays.asList(owners)), null, false, false, mirrorCol);
    }
}
