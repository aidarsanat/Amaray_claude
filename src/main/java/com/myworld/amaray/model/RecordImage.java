package com.myworld.amaray.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Дополнительная картинка записи (галерея). Главная картинка лежит в поле image самой записи. */
@Getter
@Setter
@Entity
@Table(name = "record_images", indexes = @Index(columnList = "owner_type,owner_id"))
public class RecordImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false)
    private RefType ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private String url; // /api/images/<uuid>.png

    @Column(name = "sort_pos", nullable = false)
    private int pos;
}
