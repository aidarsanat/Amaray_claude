package com.myworld.amaray.controller;

import com.myworld.amaray.dto.common.*;
import com.myworld.amaray.mapper.TagMapper;
import com.myworld.amaray.model.TagCategory;
import com.myworld.amaray.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService service;
    private final TagMapper mapper;

    // GET /api/tags?category=FACTION_TYPE: весь справочник деревом
    @GetMapping
    public ResponseEntity<List<TagNode>> tree(@RequestParam TagCategory category) {
        return ResponseEntity.ok(mapper.toTree(service.categoryTags(category)));
    }

    // GET /api/tags/suggest?category=FACTION_TYPE&q=импер
    @GetMapping("/suggest")
    public ResponseEntity<List<TagDto>> suggest(@RequestParam TagCategory category,
                                                @RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(service.suggest(category, q).stream().map(mapper::toDto).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TagDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toDto(service.get(id)));
    }

    @GetMapping("/{id}/usages")
    public ResponseEntity<List<UsageDto>> usages(@PathVariable Long id) {
        return ResponseEntity.ok(service.usages(id));
    }

    @PostMapping
    public ResponseEntity<TagDto> create(@RequestBody TagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(service.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TagDto> update(@PathVariable Long id, @RequestBody TagRequest request) {
        return ResponseEntity.ok(mapper.toDto(service.update(id, request)));
    }

    @PatchMapping("/{id}/move")
    public ResponseEntity<TagDto> move(@PathVariable Long id, @RequestBody TagMoveRequest request) {
        return ResponseEntity.ok(mapper.toDto(service.move(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
