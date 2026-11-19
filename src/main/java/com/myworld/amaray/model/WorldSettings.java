package com.myworld.amaray.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Настройки мира. В таблице всегда одна строка с id = 1. */
@Getter
@Setter
@Entity
@Table(name = "world_settings")
public class WorldSettings {

    public static final long ID = 1L;

    @Id
    private Long id = ID;

    @Column
    private Integer worldYear; // «текущий год мира», null = не задан
}
