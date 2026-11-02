package com.myworld.amaray.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Одно значение ячейки со ссылкой. Хранится у «хозяина» (owner).
 * Обычная ссылка или тег: заполнены target*.
 * Пара «тег + необязательная ссылка»: target* = тег, ref* = запись (любая часть может быть пустой).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "links", indexes = {
        @Index(columnList = "owner_type,owner_id"),
        @Index(columnList = "target_type,target_id"),
        @Index(columnList = "ref_type,ref_id")
})
public class Link {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false)
    private RefType ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "col", nullable = false)
    private String col; // имя столбца: "affiliation", "lands", "status"...

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type")
    private RefType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type")
    private RefType refType;

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "sort_pos", nullable = false)
    private int pos; // порядок внутри ячейки

    public static Link of(RefType ownerType, Long ownerId, String col, int pos) {
        Link l = new Link();
        l.ownerType = ownerType;
        l.ownerId = ownerId;
        l.col = col;
        l.pos = pos;
        return l;
    }
}
