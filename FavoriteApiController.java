package com.example.project2.controller;

import com.example.project2.model.AppUser;
import com.example.project2.model.Favorite;
import com.example.project2.model.Property;
import com.example.project2.repository.AppUserRepository;
import com.example.project2.repository.FavoriteRepository;
import com.example.project2.repository.PropertyRepository;
import com.example.project2.service.AdminLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/api/favorites")
public class FavoriteApiController {

    private final FavoriteRepository favoriteRepository;
    private final AppUserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final AdminLogService adminLogService;

    public FavoriteApiController(FavoriteRepository favoriteRepository, AppUserRepository userRepository, PropertyRepository propertyRepository, AdminLogService adminLogService) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.adminLogService = adminLogService;
    }

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyFavorites() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<Property> favorites = favoriteRepository.findByUser(userOpt.get())
                .stream()
                .map(Favorite::getProperty)
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("properties", favorites);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/toggle/{propertyId}")
    public ResponseEntity<Map<String, Object>> toggleFavorite(@PathVariable Long propertyId) {
        Map<String, Object> response = new HashMap<>();
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("requiresAuth", true);
            response.put("message", "Необходима авторизация");
            return ResponseEntity.status(401).body(response);
        }
        
        // Проверяем, не является ли пользователь администратором
        AppUser user = userOpt.get();
        if (user.getRole() != null && user.getRole().name().equals("ADMIN")) {
            response.put("success", false);
            response.put("message", "Администраторы не могут добавлять в избранное");
            return ResponseEntity.badRequest().body(response);
        }
        
        Optional<Property> propertyOpt = propertyRepository.findById(propertyId);
        if (propertyOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Объявление не найдено");
            return ResponseEntity.badRequest().body(response);
        }
        
        Property property = propertyOpt.get();
        
        Optional<Favorite> existingFavorite = favoriteRepository.findByUserAndProperty(user, property);
        
        if (existingFavorite.isPresent()) {
            favoriteRepository.delete(existingFavorite.get());
            response.put("success", true);
            response.put("favorited", false);
            response.put("message", "Удалено из избранного");
            adminLogService.log(user, "FAVORITE_REMOVE", "PROPERTY", property.getId(), null);
        } else {
            Favorite favorite = new Favorite(user, property);
            favoriteRepository.save(favorite);
            response.put("success", true);
            response.put("favorited", true);
            response.put("message", "Добавлено в избранное");
            adminLogService.log(user, "FAVORITE_ADD", "PROPERTY", property.getId(), null);
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check/{propertyId}")
    public ResponseEntity<Map<String, Object>> checkFavorite(@PathVariable Long propertyId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("favorited", false);
            return ResponseEntity.ok(response);
        }
        
        Optional<Property> propertyOpt = propertyRepository.findById(propertyId);
        if (propertyOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        boolean isFavorited = favoriteRepository.findByUserAndProperty(userOpt.get(), propertyOpt.get()).isPresent();
        
        Map<String, Object> response = new HashMap<>();
        response.put("favorited", isFavorited);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/my-ids")
    public ResponseEntity<Map<String, Object>> getMyFavoriteIds() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        
        Map<String, Object> response = new HashMap<>();
        
        if (userOpt.isEmpty()) {
            response.put("favoriteIds", List.of());
            return ResponseEntity.ok(response);
        }
        
        AppUser user = userOpt.get();
        List<Long> favoriteIds = favoriteRepository.findByUser(user)
                .stream()
                .map(f -> f.getProperty().getId())
                .collect(Collectors.toList());
        
        response.put("favoriteIds", favoriteIds);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{favoriteId}")
    public ResponseEntity<Map<String, Object>> removeFavorite(@PathVariable Long favoriteId) {
        favoriteRepository.deleteById(favoriteId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
}

