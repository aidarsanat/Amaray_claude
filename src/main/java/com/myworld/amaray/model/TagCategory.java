package com.myworld.amaray.model;

/** Справочники. Категория соответствует столбцу, а не сущности. */
public enum TagCategory {
    AGE_RANGE, CHARACTER_POWER, FACTION_POWER, GENDER, APPEARANCE_RATING,
    FACTION_TYPE, FACTION_STATUS,
    ROLE_TYPE, ROLE_RATING,
    RACE_TYPE, RACE_REPRODUCTION, RACE_ORIGIN_CAUSE,
    LAND_TYPE, LAND_STYLE,
    SETTLEMENT_TYPE, SETTLEMENT_STYLE,
    PLACE_TYPE,
    ITEM_TYPE, ITEM_QUALITY,
    EVENT_TYPE,
    OTHER;

    /**
     * Справочник типа: его папки служат папками-типами для выбора записей своей таблицы в других столбцах
     * (например, LAND_TYPE в Role → Земли). Такие папки создаются невыбираемыми.
     */
    public boolean isTypeDictionary() {
        return switch (this) {
            case FACTION_TYPE, ROLE_TYPE, RACE_TYPE, LAND_TYPE, SETTLEMENT_TYPE,
                 PLACE_TYPE, ITEM_TYPE, EVENT_TYPE -> true;
            default -> false;
        };
    }
}
