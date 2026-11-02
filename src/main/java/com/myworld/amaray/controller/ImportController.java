package com.myworld.amaray.controller;

import com.myworld.amaray.exception.BadRequestException;
import com.myworld.amaray.model.RefType;
import com.myworld.amaray.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService service;

    // POST /api/import/csv?entity=races, multipart-поле file
    @PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportService.Result> importCsv(@RequestParam String entity,
                                                          @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new BadRequestException("Файл пуст");
        return ResponseEntity.ok(service.importCsv(RefType.fromPath(entity), file.getBytes()));
    }
}
