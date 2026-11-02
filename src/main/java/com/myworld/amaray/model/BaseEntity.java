package com.myworld.amaray.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/** Общая основа всех таблиц мира. Связи и текст ячеек лежат отдельно (Link, CellText). */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SyncState syncState = SyncState.PENDING;

    /** Как запись называется в плашках и подсказках. */
    public abstract String displayName();

    /** Единственное обязательное поле (название или имя). */
    public abstract String mainName();

    /** Строка для поиска по подсказкам. */
    public String searchText() {
        return displayName();
    }

    /** Простые поля записи (не связи) для JSON. */
    public abstract Map<String, Object> toFields();

    /** Применяет только те ключи, которые есть в карте. */
    public abstract void applyFields(Map<String, Object> fields);
}
