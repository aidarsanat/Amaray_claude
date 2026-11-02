package com.myworld.amaray.controller;

import com.myworld.amaray.dto.common.UsageDto;
import com.myworld.amaray.dto.record.RecordDto;
import com.myworld.amaray.dto.record.RecordRequest;
import com.myworld.amaray.mapper.RecordMapper;
import com.myworld.amaray.model.BaseEntity;
import com.myworld.amaray.model.RefType;
import com.myworld.amaray.service.ListQuery;
import com.myworld.amaray.service.PageResult;
import com.myworld.amaray.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Один контроллер на все девять таблиц: /api/characters, /api/factions, /api/roles, /api/races,
 * /api/lands, /api/settlements, /api/places, /api/items, /api/events.
 * Конкретные пути (/api/tags, /api/images...) имеют приоритет над {entity}.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService service;
    private final RecordMapper mapper;

    @GetMapping("/{entity}")
    public ResponseEntity<List<RecordDto>> list(@PathVariable String entity, @RequestParam Map<String, String> params) {
        RefType type = RefType.fromPath(entity);
        PageResult<BaseEntity> page = service.list(type, ListQuery.from(params));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(page.total()))
                .body(mapper.toDtos(type, page.items(), false));
    }

    @GetMapping("/{entity}/{id}")
    public ResponseEntity<RecordDto> get(@PathVariable String entity, @PathVariable Long id) {
        RefType type = RefType.fromPath(entity);
        return ResponseEntity.ok(mapper.toDto(type, service.get(type, id), true));
    }

    @GetMapping("/{entity}/pending")
    public ResponseEntity<List<RecordDto>> pending(@PathVariable String entity) {
        RefType type = RefType.fromPath(entity);
        return ResponseEntity.ok(mapper.toDtos(type, service.pending(type), false));
    }

    @GetMapping("/{entity}/{id}/usages")
    public ResponseEntity<List<UsageDto>> usages(@PathVariable String entity, @PathVariable Long id) {
        return ResponseEntity.ok(service.usages(RefType.fromPath(entity), id));
    }

    @PostMapping("/{entity}")
    public ResponseEntity<RecordDto> create(@PathVariable String entity, @RequestBody RecordRequest request) {
        RefType type = RefType.fromPath(entity);
        BaseEntity created = service.create(type, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(type, created, true));
    }

    @PutMapping("/{entity}/{id}")
    public ResponseEntity<RecordDto> update(@PathVariable String entity, @PathVariable Long id,
                                            @RequestBody RecordRequest request) {
        RefType type = RefType.fromPath(entity);
        return ResponseEntity.ok(mapper.toDto(type, service.update(type, id, request), true));
    }

    @DeleteMapping("/{entity}/{id}")
    public ResponseEntity<Void> delete(@PathVariable String entity, @PathVariable Long id) {
        service.delete(RefType.fromPath(entity), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{entity}/{id}/sync")
    public ResponseEntity<RecordDto> sync(@PathVariable String entity, @PathVariable Long id) {
        RefType type = RefType.fromPath(entity);
        return ResponseEntity.ok(mapper.toDto(type, service.markSynced(type, id), false));
    }
}
