package com.example.project2.controller;

import com.example.project2.model.AppUser;
import com.example.project2.model.UserSettings;
import com.example.project2.repository.AppUserRepository;
import com.example.project2.repository.UserSettingsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/api/user-settings")
public class UserSettingsController {

    private final UserSettingsRepository settingsRepository;
    private final AppUserRepository userRepository;

    public UserSettingsController(UserSettingsRepository settingsRepository, AppUserRepository userRepository) {
        this.settingsRepository = settingsRepository;
        this.userRepository = userRepository;
    }

    private Optional<AppUser> currentUser(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) return Optional.empty();
        return userRepository.findByEmail(auth.getName());
    }

    @GetMapping
    public ResponseEntity<?> get(){
        Optional<AppUser> u = currentUser();
        if (u.isEmpty()) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(settingsRepository.findByUser(u.get()).orElseGet(UserSettings::new));
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody Map<String,Object> payload){
        Optional<AppUser> u = currentUser();
        if (u.isEmpty()) return ResponseEntity.status(401).build();
        AppUser user = u.get();
        UserSettings s = settingsRepository.findByUser(user).orElseGet(UserSettings::new);
        s.setUser(user);
        s.setTheme((String) payload.getOrDefault("theme", s.getTheme()));
        s.setLocale((String) payload.getOrDefault("locale", s.getLocale()));
        s.setNumberFormat((String) payload.getOrDefault("numberFormat", s.getNumberFormat()));
        Object ps = payload.get("pageSize");
        if (ps != null) {
            try { s.setPageSize(Integer.parseInt(ps.toString())); } catch (Exception ignored) {}
        }
        s.setSavedFilters((String) payload.getOrDefault("savedFilters", s.getSavedFilters()));
        return ResponseEntity.ok(settingsRepository.save(s));
    }
}
