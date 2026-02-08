package com.example.project2.controller;

import com.example.project2.model.*;
import com.example.project2.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/api/deal")
public class DealApiController {

    private final DealRepository dealRepository;
    private final AppUserRepository userRepository;
    private final PropertyRepository propertyRepository;

    public DealApiController(DealRepository dealRepository, AppUserRepository userRepository, PropertyRepository propertyRepository) {
        this.dealRepository = dealRepository;
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
    }

    private boolean isParticipant(Deal deal, Long userId) {
        return (deal.getBuyer() != null && deal.getBuyer().getId().equals(userId))
                || (deal.getSeller() != null && deal.getSeller().getId().equals(userId))
                || (deal.getRealtor() != null && deal.getRealtor().getId().equals(userId));
    }

    @PostMapping("/{dealId}/accept")
    @Transactional
    public ResponseEntity<Map<String, Object>> accept(@PathVariable Long dealId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.status(401).build();

        Optional<Deal> dealOpt = dealRepository.findById(dealId);
        if (dealOpt.isEmpty()) return ResponseEntity.notFound().build();

        Deal deal = dealOpt.get();
        Long userId = userOpt.get().getId();
        if (!isParticipant(deal, userId)) return ResponseEntity.status(403).build();

        // Принимает только покупатель
        if (deal.getBuyer() == null || !deal.getBuyer().getId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Принять договор может только покупатель"));
        }

        // Меняем статус сделки и объекта
        deal.setStatus(DealStatus.completed);
        deal.setDealDate(java.time.LocalDateTime.now());
        dealRepository.save(deal);

        Property property = deal.getProperty();
        if (property != null) {
            property.setStatus(PropertyStatus.sold);
            propertyRepository.save(property);
        }

        return ResponseEntity.ok(Map.of("success", true, "redirect", "/deals"));
    }

    @PostMapping("/{dealId}/decline")
    @Transactional
    public ResponseEntity<Map<String, Object>> decline(@PathVariable Long dealId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.status(401).build();

        Optional<Deal> dealOpt = dealRepository.findById(dealId);
        if (dealOpt.isEmpty()) return ResponseEntity.notFound().build();

        Deal deal = dealOpt.get();
        Long userId = userOpt.get().getId();
        if (!isParticipant(deal, userId)) return ResponseEntity.status(403).build();

        // Отклонять может покупатель
        if (deal.getBuyer() == null || !deal.getBuyer().getId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Отклонить договор может только покупатель"));
        }

        // Фиксируем отказ как статус confirmed (без продажи)
        deal.setStatus(DealStatus.confirmed);
        deal.setDealDate(java.time.LocalDateTime.now());
        dealRepository.save(deal);
        return ResponseEntity.ok(Map.of("success", true, "redirect", "/deals"));
    }
}
