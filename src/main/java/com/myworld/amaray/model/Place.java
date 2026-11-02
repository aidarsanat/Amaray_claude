package com.myworld.amaray.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "places")
public class Place extends NamedEntity {

    @Column(columnDefinition = "TEXT")
    private String features; // особенности

    @Override
    protected void putExtra(Map<String, Object> m) {
        m.put("features", features);
    }

    @Override
    protected void readExtra(Map<String, Object> f) {
        if (f.containsKey("features")) features = Fields.text(f.get("features"));
    }
}
