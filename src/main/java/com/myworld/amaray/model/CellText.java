package com.myworld.amaray.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Свободный текст ячейки, парный к ссылкам столбца («фракция, похожая на викингов»). */
@Getter
@Setter
@Entity
@Table(name = "cell_texts", uniqueConstraints =
        @UniqueConstraint(columnNames = {"owner_type", "owner_id", "col"}))
public class CellText {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false)
    private RefType ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "col", nullable = false)
    private String col;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
}
