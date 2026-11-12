package com.myworld.amaray.controller;

import com.myworld.amaray.dto.doc.DocDto;
import com.myworld.amaray.dto.doc.DocMoveRequest;
import com.myworld.amaray.dto.doc.DocNode;
import com.myworld.amaray.dto.doc.DocRequest;
import com.myworld.amaray.service.DocService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/docs")
@RequiredArgsConstructor
public class DocController {

    private final DocService service;

    // GET /api/docs: всё дерево папок и документов без текстов
    @GetMapping
    public ResponseEntity<List<DocNode>> tree() {
        return ResponseEntity.ok(service.tree());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(DocDto.of(service.get(id), true));
    }

    @PostMapping
    public ResponseEntity<DocDto> create(@RequestBody DocRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(DocDto.of(service.create(request), true));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocDto> update(@PathVariable Long id, @RequestBody DocRequest request) {
        return ResponseEntity.ok(DocDto.of(service.update(id, request), true));
    }

    @PatchMapping("/{id}/move")
    public ResponseEntity<DocDto> move(@PathVariable Long id, @RequestBody DocMoveRequest request) {
        return ResponseEntity.ok(DocDto.of(service.move(id, request), false));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
