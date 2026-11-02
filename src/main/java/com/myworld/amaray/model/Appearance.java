package com.myworld.amaray.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class Appearance {

    @Column
    private Integer height; // см

    @Column(length = 7)
    private String skinColor; // "#RRGGBB"

    @Column(length = 7)
    private String hairColor;

    @Column(length = 7)
    private String eyeColor;

    @Column(columnDefinition = "TEXT")
    private String features; // особенности внешности
}
