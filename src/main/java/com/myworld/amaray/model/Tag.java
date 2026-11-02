package com.myworld.amaray.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Тег или папка справочника. Три вида:
 * папка-класс (folder, не selectable), папка-тип (folder, selectable), тег (не folder).
 */
@Getter
@Setter
@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Уникальность в пределах категории и папки проверяет TagService
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TagCategory category;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Tag parent; // null = корень справочника

    @Column(nullable = false)
    private boolean folder;

    @Column(nullable = false)
    private boolean selectable = true;

    @Column
    private String color; // null = берётся у ближайшего родителя

    @Column(nullable = false)
    private int sortOrder;
}
