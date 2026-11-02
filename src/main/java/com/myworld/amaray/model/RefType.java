package com.myworld.amaray.model;

import com.myworld.amaray.exception.NotFoundException;

/** Тип ссылки: тег или одна из таблиц. Нужен, потому что фракция с id 1 и персонаж с id 1 это разные записи. */
public enum RefType {
    TAG(null),
    CHARACTER("characters"),
    FACTION("factions"),
    ROLE("roles"),
    RACE("races"),
    LAND("lands"),
    SETTLEMENT("settlements"),
    PLACE("places"),
    ITEM("items"),
    EVENT("events");

    private final String path;

    RefType(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

    public boolean isRecord() {
        return this != TAG;
    }

    public static RefType fromPath(String path) {
        for (RefType t : values()) {
            if (t.path != null && t.path.equals(path)) return t;
        }
        throw new NotFoundException("Неизвестная таблица: " + path);
    }
}
