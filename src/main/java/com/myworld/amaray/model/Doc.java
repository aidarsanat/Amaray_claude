package com.myworld.amaray.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Документ или папка для лора и историй. Не привязан к записям мира. */
@Getter
@Setter
@Entity
@Table(name = "documents")
public class Doc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content; // HTML из Tiptap, у папки null

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Doc parent; // null = корень

    @Column(nullable = false)
    private boolean folder;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}