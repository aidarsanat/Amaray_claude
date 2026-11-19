package com.myworld.amaray.controller;

import com.myworld.amaray.dto.common.WorldYearDto;
import com.myworld.amaray.service.WorldSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final WorldSettingsService service;

    // GET /api/settings/world-year → {"year": 1250}
    @GetMapping("/world-year")
    public ResponseEntity<WorldYearDto> worldYear() {
        return ResponseEntity.ok(new WorldYearDto(service.worldYear()));
    }

    // PUT /api/settings/world-year с телом {"year": 1250}; {"year": null} очищает год
    @PutMapping("/world-year")
    public ResponseEntity<WorldYearDto> setWorldYear(@RequestBody WorldYearDto body) {
        return ResponseEntity.ok(new WorldYearDto(service.setWorldYear(body == null ? null : body.year())));
    }
}