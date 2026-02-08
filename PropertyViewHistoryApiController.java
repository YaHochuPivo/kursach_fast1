package com.example.project2.controller;

import com.example.project2.model.AppUser;
import com.example.project2.model.Property;
import com.example.project2.model.PropertyViewHistory;
import com.example.project2.repository.AppUserRepository;
import com.example.project2.repository.PropertyRepository;
import com.example.project2.repository.PropertyViewHistoryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/api/view-history")
public class PropertyViewHistoryApiController {

    private final PropertyViewHistoryRepository viewHistoryRepository;
    private final PropertyRepository propertyRepository;
    private final AppUserRepository userRepository;

    public PropertyViewHistoryApiController(
            PropertyViewHistoryRepository viewHistoryRepository,
            PropertyRepository propertyRepository,
            AppUserRepository userRepository) {
        this.viewHistoryRepository = viewHistoryRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/{propertyId}")
    @Transactional
    public ResponseEntity<?> recordView(@PathVariable Long propertyId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return ResponseEntity.ok().build(); // Игнорируем для неавторизованных пользователей
        }

        Optional<AppUser> userOpt = userRepository.findByEmail(auth.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok().build();
        }

        Optional<Property> propertyOpt = propertyRepository.findById(propertyId);
        if (propertyOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AppUser user = userOpt.get();
        Property property = propertyOpt.get();

        Optional<PropertyViewHistory> existingHistoryOpt = viewHistoryRepository.findByUserIdAndPropertyId(user.getId(), propertyId);
        
        if (existingHistoryOpt.isPresent()) {
            PropertyViewHistory history = existingHistoryOpt.get();
            history.setViewedAt(LocalDateTime.now());
            history.setViewCount(history.getViewCount() + 1);
            viewHistoryRepository.save(history);
        } else {
            PropertyViewHistory history = new PropertyViewHistory(user, property);
            viewHistoryRepository.save(history);
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/recent")
    @Transactional
    public ResponseEntity<List<PropertyViewHistoryResponse>> getRecentViews(@RequestParam(defaultValue = "10") int limit) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return ResponseEntity.ok(List.of());
        }

        Optional<AppUser> userOpt = userRepository.findByEmail(auth.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        Pageable pageable = PageRequest.of(0, limit);
        List<PropertyViewHistory> histories = viewHistoryRepository.findRecentByUserId(userOpt.get().getId(), pageable);
        List<PropertyViewHistoryResponse> responses = histories.stream()
                .map(h -> {
                    Property p = h.getProperty();
                    // JOIN FETCH уже загрузил property, но проверяем на null для безопасности
                    return new PropertyViewHistoryResponse(
                            p != null ? p.getId() : null,
                            p != null ? p.getAddress() : null,
                            p != null ? p.getCity() : null,
                            p != null ? p.getPrice() : null,
                            p != null ? p.getArea() : null,
                            p != null ? p.getType() : null,
                            p != null && p.getImageUrls() != null && !p.getImageUrls().isEmpty() 
                                    ? p.getImageUrls().split(",")[0].trim() 
                                    : null,
                            h.getViewedAt()
                    );
                })
                .filter(r -> r.getPropertyId() != null) // Фильтруем null значения
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    public static class PropertyViewHistoryResponse {
        private Long propertyId;
        private String address;
        private String city;
        private Float price;
        private Float area;
        private String type;
        private String imageUrl;
        private LocalDateTime viewedAt;

        public PropertyViewHistoryResponse(Long propertyId, String address, String city, Float price, 
                                         Float area, String type, String imageUrl, LocalDateTime viewedAt) {
            this.propertyId = propertyId;
            this.address = address;
            this.city = city;
            this.price = price;
            this.area = area;
            this.type = type;
            this.imageUrl = imageUrl;
            this.viewedAt = viewedAt;
        }

        // Getters
        public Long getPropertyId() { return propertyId; }
        public String getAddress() { return address; }
        public String getCity() { return city; }
        public Float getPrice() { return price; }
        public Float getArea() { return area; }
        public String getType() { return type; }
        public String getImageUrl() { return imageUrl; }
        public LocalDateTime getViewedAt() { return viewedAt; }
    }
}

