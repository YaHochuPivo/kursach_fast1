package com.example.project2.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/v1/api/async")
@Tag(name = "Async Operations", description = "Асинхронные операции для улучшения производительности")
public class AsyncApiController {

    @GetMapping("/health")
    @Operation(summary = "Проверка здоровья системы", description = "Асинхронная проверка состояния системы")
    public CompletableFuture<ResponseEntity<String>> healthCheck() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(100); // Имитация асинхронной работы
                return ResponseEntity.ok("System is healthy");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ResponseEntity.internalServerError().body("System check failed");
            }
        });
    }
}
