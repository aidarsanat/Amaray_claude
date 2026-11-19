package com.myworld.amaray.service;

import com.myworld.amaray.dto.record.AgeAtEvent;
import com.myworld.amaray.exception.BadRequestException;
import com.myworld.amaray.model.Characters;
import com.myworld.amaray.repository.RecordStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChronologyService {

    private final RecordStore store;

    /** Возраст на любой год. Пригодится и для плашки «текущий год мира». */
    public static AgeAtEvent ageAt(Characters c, int year) {
        if (c.getBirthYear() == null) throw new BadRequestException("У персонажа не указан год рождения");
        if (year < c.getBirthYear()) return new AgeAtEvent(null, "NOT_BORN");
        if (c.getDeathYear() != null && year > c.getDeathYear()) {
            return new AgeAtEvent(c.getDeathYear() - c.getBirthYear(), "DEAD");
        }
        return new AgeAtEvent(year - c.getBirthYear(), "ALIVE");
    }
}
