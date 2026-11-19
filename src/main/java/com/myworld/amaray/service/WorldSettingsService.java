package com.myworld.amaray.service;

import com.myworld.amaray.model.WorldSettings;
import com.myworld.amaray.repository.WorldSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * «Текущий год мира». Хранится одной строкой, возраст персонажей по нему считается при чтении
 * и в базу не записывается, поэтому смена года не трогает сами записи.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class WorldSettingsService {

    private final WorldSettingsRepository repo;

    @Transactional(readOnly = true)
    public Integer worldYear() {
        return repo.findById(WorldSettings.ID).map(WorldSettings::getWorldYear).orElse(null);
    }

    /** null очищает год: возраст снова берётся из ручного поля. */
    public Integer setWorldYear(Integer year) {
        WorldSettings s = repo.findById(WorldSettings.ID).orElseGet(WorldSettings::new);
        s.setWorldYear(year);
        repo.save(s);
        return year;
    }
}
