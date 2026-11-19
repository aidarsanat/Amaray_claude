package com.myworld.amaray.service;

import com.myworld.amaray.dto.record.CharacterAge;
import com.myworld.amaray.model.Characters;

/** Расчёт возраста персонажа по «текущему году мира». */
public final class ChronologyService {

    private ChronologyService() {}

    /**
     * worldYear = null: год мира не задан, берётся ручной возраст.
     * Без года рождения тоже берётся ручной возраст.
     */
    public static CharacterAge ageAt(Characters c, Integer worldYear) {
        if (worldYear == null || c.getBirthYear() == null) {
            return new CharacterAge(c.getAge(), "MANUAL");
        }
        if (worldYear < c.getBirthYear()) return new CharacterAge(null, "NOT_BORN");
        if (c.getDeathYear() != null && worldYear > c.getDeathYear()) {
            return new CharacterAge(c.getDeathYear() - c.getBirthYear(), "DEAD");
        }
        return new CharacterAge(worldYear - c.getBirthYear(), "ALIVE");
    }
}
