package com.example.project2.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/v1/api/uploads")
public class UploadApiController {

    private final Path root = Paths.get("uploads");

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/images")
    public ResponseEntity<?> uploadImages(@RequestParam("files") List<MultipartFile> files) {
        try {
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }
            String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            List<String> urls = new ArrayList<>();
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                if (file.isEmpty()) continue;
                String original = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : ("image_" + i + ".jpg"));
                String filename = datePrefix + "_" + i + "_" + original;
                Path dest = root.resolve(filename);
                Files.copy(file.getInputStream(), dest);
                urls.add("/uploads/" + filename);
            }
            return ResponseEntity.ok(urls);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload error: " + e.getMessage());
        }
    }
}


