package com.example.project2.controller;

import com.example.project2.model.AppUser;
import com.example.project2.model.Deal;
import com.example.project2.model.DealStatus;
import com.example.project2.model.Property;
import com.example.project2.repository.AppUserRepository;
import com.example.project2.repository.DealRepository;
import com.example.project2.repository.PropertyRepository;
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
@RequestMapping("/v1/api/realtor")
public class RealtorApiController {

    private final DealRepository dealRepository;
    private final PropertyRepository propertyRepository;
    private final AppUserRepository userRepository;

    public RealtorApiController(DealRepository dealRepository, PropertyRepository propertyRepository, AppUserRepository userRepository) {
        this.dealRepository = dealRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getRealtorStats() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        AppUser realtor = userOpt.get();
        List<Deal> allDeals = dealRepository.findByRealtorId(realtor.getId());
        
        // Статистика по сделкам
        long totalDeals = allDeals.size();
        long completedDeals = allDeals.stream()
                .filter(d -> d.getStatus() == DealStatus.completed)
                .count();
        long pendingDeals = allDeals.stream()
                .filter(d -> d.getStatus() == DealStatus.pending)
                .count();
        // В нашей модели отказ = confirmed
        long cancelledDeals = allDeals.stream()
                .filter(d -> d.getStatus() == DealStatus.confirmed)
                .count();

        // Общий доход риелтора = количество завершенных сделок * ставка риелтора
        java.math.BigDecimal fee = realtor.getServiceFee() != null ? realtor.getServiceFee() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalIncomeBD = fee.multiply(new java.math.BigDecimal(completedDeals));
        double totalRevenue = totalIncomeBD.doubleValue();
        
        // Статистика по месяцам (последние 6 месяцев)
        Map<String, Long> monthlyStats = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            int month = java.time.LocalDateTime.now().minusMonths(i).getMonthValue();
            int year = java.time.LocalDateTime.now().minusMonths(i).getYear();
            String monthKey = year + "-" + String.format("%02d", month);
            
            long count = allDeals.stream()
                    .filter(d -> d.getStatus() == DealStatus.completed)
                    .filter(d -> d.getDealDate() != null)
                    .filter(d -> {
                        int dealMonth = d.getDealDate().getMonthValue();
                        int dealYear = d.getDealDate().getYear();
                        return dealMonth == month && dealYear == year;
                    })
                    .count();
            
            monthlyStats.put(monthKey, count);
        }
        
        // Статистика по объявлениям риелтора
        // Объявления, где риелтор является автором (user) или назначен как realtor
        List<Property> realtorProperties = propertyRepository.findAll().stream()
                .filter(p -> {
                    // Проверяем, что объявление принадлежит риелтору (как user) или назначено ему (как realtor)
                    boolean isOwner = p.getUser() != null && p.getUser().getId().equals(realtor.getId());
                    boolean isAssignedRealtor = p.getRealtor() != null && p.getRealtor().getId().equals(realtor.getId());
                    return isOwner || isAssignedRealtor;
                })
                .collect(Collectors.toList());
        
        long totalProperties = realtorProperties.size();
        long activeProperties = realtorProperties.stream()
                .filter(p -> p.getStatus().toString().equals("active"))
                .count();
        long promotedProperties = realtorProperties.stream()
                .filter(p -> p.getPromoted() != null && p.getPromoted())
                .count();
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalDeals", totalDeals);
        response.put("completedDeals", completedDeals);
        response.put("pendingDeals", pendingDeals);
        response.put("cancelledDeals", cancelledDeals);
        response.put("totalRevenue", totalRevenue);
        response.put("serviceFee", fee);
        response.put("monthlyStats", monthlyStats);
        response.put("totalProperties", totalProperties);
        response.put("activeProperties", activeProperties);
        response.put("promotedProperties", promotedProperties);
        
        return ResponseEntity.ok(response);
    }
}

