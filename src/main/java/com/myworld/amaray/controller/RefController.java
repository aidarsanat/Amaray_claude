package com.myworld.amaray.controller;

import com.myworld.amaray.dto.common.RefDto;
import com.myworld.amaray.model.RefType;
import com.myworld.amaray.service.RefService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/refs")
@RequiredArgsConstructor
public class RefController {

    private final RefService service;

    // GET /api/refs/suggest?types=LAND,SETTLEMENT&q=дол&typeTagId=7
    @GetMapping("/suggest")
    public ResponseEntity<List<RefDto>> suggest(@RequestParam List<RefType> types,
                                                @RequestParam(defaultValue = "") String q,
                                                @RequestParam(required = false) Long typeTagId) {
        return ResponseEntity.ok(service.suggest(types, q, typeTagId));
    }
}
