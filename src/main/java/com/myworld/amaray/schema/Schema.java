package com.myworld.amaray.schema;

import com.myworld.amaray.exception.BadRequestException;
import com.myworld.amaray.model.RefType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.myworld.amaray.model.RefType.*;
import static com.myworld.amaray.model.TagCategory.*;
import static com.myworld.amaray.schema.ColumnSpec.*;

/** Единый перечень столбцов со связями. Это таблица связей из дока в виде кода. */
public final class Schema {

    private static final Map<RefType, List<ColumnSpec>> COLUMNS = new EnumMap<>(RefType.class);

    static {
        COLUMNS.put(CHARACTER, List.of(
                refs("affiliation", false, FACTION),
                refs("race", false, RACE),
                refs("role", false, ROLE),
                tags("ageRange", AGE_RANGE, true, false),
                tags("power", CHARACTER_POWER, false, true), // прогресс по порядку: A, A+, S-
                tags("gender", GENDER, true, false),
                tags("appearanceRating", APPEARANCE_RATING, true, false)));

        COLUMNS.put(FACTION, List.of(
                refs("leaders", false, CHARACTER),
                mirror("lands", "affiliation", LAND, SETTLEMENT), // хозяева: Land и Settlement
                refs("origin", false, FACTION),
                pairs("status", FACTION_STATUS, false, FACTION),
                tags("type", FACTION_TYPE, false, false),
                tags("power", FACTION_POWER, false, false)));

        COLUMNS.put(ROLE, List.of(
                refs("lands", false, LAND),
                refs("affiliation", false, FACTION),
                tags("type", ROLE_TYPE, false, false),
                tags("rating", ROLE_RATING, false, false)));

        COLUMNS.put(RACE, List.of(
                refs("lands", false, LAND),
                pairs("origin", RACE_ORIGIN_CAUSE, true, RACE), // ссылка = родитель, тег = причина
                pairs("reproduction", RACE_REPRODUCTION, false, RACE),
                tags("type", RACE_TYPE, false, false)));

        COLUMNS.put(LAND, List.of(
                refs("affiliation", false, FACTION),
                tags("type", LAND_TYPE, false, false),
                tags("style", LAND_STYLE, false, false)));

        COLUMNS.put(SETTLEMENT, List.of(
                refs("lands", false, LAND),
                refs("affiliation", false, FACTION),
                tags("type", SETTLEMENT_TYPE, false, false),
                tags("style", SETTLEMENT_STYLE, false, false)));

        COLUMNS.put(PLACE, List.of(
                refs("lands", true, LAND, SETTLEMENT),
                refs("affiliation", false, FACTION),
                tags("type", PLACE_TYPE, false, false)));

        // Предметы: артефактность и качества ещё не продуманы, пока обычные теги
        COLUMNS.put(ITEM, List.of(
                tags("type", ITEM_TYPE, false, false),
                refs("owner", false, CHARACTER),
                refs("creator", false, FACTION, CHARACTER),
                refs("location", true, LAND, FACTION, PLACE, SETTLEMENT),
                tags("quality", ITEM_QUALITY, false, false)));

        COLUMNS.put(EVENT, List.of(
                refs("participants", false, FACTION, CHARACTER),
                tags("ageRange", AGE_RANGE, true, false),
                tags("type", EVENT_TYPE, false, false)));
    }

    private Schema() {}

    public static List<ColumnSpec> columns(RefType type) {
        return COLUMNS.getOrDefault(type, List.of());
    }

    public static Optional<ColumnSpec> find(RefType type, String field) {
        return columns(type).stream().filter(c -> c.field().equals(field)).findFirst();
    }

    public static ColumnSpec require(RefType type, String field) {
        return find(type, field).orElseThrow(() ->
                new BadRequestException("У таблицы " + type.path() + " нет столбца '" + field + "'"));
    }
}
