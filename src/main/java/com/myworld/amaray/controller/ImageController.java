package com.myworld.amaray.controller;

import com.myworld.amaray.exception.BadRequestException;
import com.myworld.amaray.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/** Картинки для текстового редактора лежат файлами на диске, в БД только ссылка. */
@RestController
@RequestMapping("/api/images")
public class ImageController {

    private static final Map<String, String> TYPES = Map.of(
            "png", MediaType.IMAGE_PNG_VALUE,
            "jpg", MediaType.IMAGE_JPEG_VALUE,
            "jpeg", MediaType.IMAGE_JPEG_VALUE,
            "gif", MediaType.IMAGE_GIF_VALUE,
            "webp", "image/webp");

    private final Path root;

    public ImageController(@Value("${app.images.dir:./data/images}") String dir) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot < 0 ? "" : original.substring(dot + 1).toLowerCase();
        if (file.isEmpty() || !TYPES.containsKey(ext)) {
            throw new BadRequestException("Нужен файл png, jpg, gif или webp");
        }
        Files.createDirectories(root);
        String name = UUID.randomUUID() + "." + ext;
        file.transferTo(root.resolve(name));
        return ResponseEntity.ok(Map.of("url", "/api/images/" + name));
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> get(@PathVariable String filename) {
        Path p = root.resolve(filename).normalize();
        if (!p.startsWith(root) || !Files.isRegularFile(p)) throw new NotFoundException("Картинка не найдена");
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        String type = TYPES.getOrDefault(ext, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(type)).body(new FileSystemResource(p));
    }
}
