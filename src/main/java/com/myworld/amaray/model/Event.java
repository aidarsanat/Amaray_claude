package com.myworld.amaray.model;

import com.myworld.amaray.exception.BadRequestException;

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
        if (eventYear != null && eventEndYear != null && eventEndYear < eventYear) {
            throw new BadRequestException("Конец события раньше начала");
        }
    }
}
