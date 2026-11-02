package com.myworld.amaray.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

/** Запись с названием и описанием. */
@Getter
@Setter
@MappedSuperclass
public abstract class NamedEntity extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Override
    public String displayName() {
        return name;
    }

    @Override
    public String mainName() {
        return name;
    }

    @Override
    public Map<String, Object> toFields() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("description", description);
        putExtra(m);
        return m;
    }

    @Override
    public void applyFields(Map<String, Object> f) {
        if (f.containsKey("name")) name = Fields.str(f.get("name"));
        if (f.containsKey("description")) description = Fields.text(f.get("description"));
        readExtra(f);
    }

    /** Дополнительные поля конкретной таблицы. */
    protected void putExtra(Map<String, Object> m) {}

    protected void readExtra(Map<String, Object> f) {}
}
