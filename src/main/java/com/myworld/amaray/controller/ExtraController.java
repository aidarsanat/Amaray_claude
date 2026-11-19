package com.myworld.amaray.controller;

import com.myworld.amaray.dto.record.RaceOriginDto;
import com.myworld.amaray.dto.record.RecordDto;
import com.myworld.amaray.mapper.RecordMapper;
import com.myworld.amaray.model.RefType;
import com.myworld.amaray.service.RaceOriginService;
import com.myworld.amaray.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ExtraController {

    private final RaceOriginService raceOrigin;
    private final RecordService records;
    private final RecordMapper mapper;

    @GetMapping("/races/{id}/origin")
    public ResponseEntity<RaceOriginDto> origin(@PathVariable Long id) {
        return ResponseEntity.ok(raceOrigin.describe(id));
    }

    @GetMapping("/factions/{id}/members")
    public ResponseEntity<List<RecordDto>> members(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toDtos(RefType.CHARACTER, records.membersOfFaction(id), false));
    }
}
