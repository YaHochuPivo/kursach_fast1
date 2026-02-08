package com.example.project2.controller;

import com.example.project2.model.UserRole;
import com.example.project2.repository.AdminLogRepository;
import com.example.project2.repository.AppUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/api/admin/logs")
public class AdminLogApiController {

    private final AdminLogRepository logRepository;
    private final AppUserRepository userRepository;

    public AdminLogApiController(AdminLogRepository logRepository, AppUserRepository userRepository) {
        this.logRepository = logRepository;
        this.userRepository = userRepository;
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return false;
        String email = auth.getName();
        if (email != null && email.equalsIgnoreCase("admin@realestate.com")) return true;
        return userRepository.findByEmail(email)
                .map(u -> u.getRole() == UserRole.ADMIN)
                .orElse(false);
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(value = "email", required = false) String email,
                                  @RequestParam(value = "action", required = false) String action,
                                  @RequestParam(value = "from", required = false) String from,
                                  @RequestParam(value = "to", required = false) String to,
                                  @RequestParam(value = "page", required = false, defaultValue = "0") int page,
                                  @RequestParam(value = "size", required = false, defaultValue = "20") int size) {
        if (!isAdmin()) return ResponseEntity.status(403).build();

        java.time.LocalDateTime fromDate = null;
        java.time.LocalDateTime toDate = null;
        try { if (from != null && !from.isBlank()) fromDate = java.time.LocalDateTime.parse(from); } catch (Exception ignore) {}
        try { if (to != null && !to.isBlank()) toDate = java.time.LocalDateTime.parse(to); } catch (Exception ignore) {}

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(200, size)));
        Page<com.example.project2.model.AdminLog> result = logRepository.search(
                (email != null && !email.isBlank()) ? email : null,
                (action != null && !action.isBlank()) ? action : null,
                fromDate,
                toDate,
                pageable
        );

        return ResponseEntity.ok(Map.of(
                "logs", result.getContent(),
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalPages", result.getTotalPages(),
                "totalElements", result.getTotalElements()
        ));
    }
}
