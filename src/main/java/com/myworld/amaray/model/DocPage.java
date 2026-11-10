package com.myworld.amaray.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "doc_pages", indexes = @Index(columnList = "owner_type,owner_id"))
public class DocPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content; // HTML из Tiptap

    // оба null = самостоятельный документ, как в Google Docs
    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type")
    private RefType ownerType;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column
    private int sortOrder;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
