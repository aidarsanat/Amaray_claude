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
@Table(name = "events")
public class Event extends NamedEntity {

    // Точные годы. Неточный период (поколение) это тег AGE_RANGE в столбце "ageRange".
    @Column
    private Integer eventYear;

    @Column
    private Integer eventEndYear;

    @Override
    protected void putExtra(Map<String, Object> m) {
        m.put("year", eventYear);
        m.put("endYear", eventEndYear);
    }

    @Override
    protected void readExtra(Map<String, Object> f) {
        if (f.containsKey("year")) eventYear = Fields.integer(f.get("year"), "year");
        if (f.containsKey("endYear")) eventEndYear = Fields.integer(f.get("endYear"), "endYear");
    }
}
