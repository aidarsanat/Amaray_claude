package com.myworld.amaray.service;

import com.myworld.amaray.exception.BadRequestException;
import com.myworld.amaray.model.Characters;
import com.myworld.amaray.model.Event;
import com.myworld.amaray.model.RefType;
import com.myworld.amaray.repository.RecordStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChronologyService {

    private final RecordStore store;

    /** Сколько лет было любому персонажу (не только участнику) на момент события. Отрицательное число: ещё не родился. */
    public int characterAgeAtEvent(Long eventId, Long characterId) {
        Event event = (Event) store.require(RefType.EVENT, eventId);
        Characters character = (Characters) store.require(RefType.CHARACTER, characterId);
        if (event.getEventYear() == null) throw new BadRequestException("У события не указан год");
        if (character.getBirthYear() == null) throw new BadRequestException("У персонажа не указан год рождения");
        return event.getEventYear() - character.getBirthYear();
    }
}
