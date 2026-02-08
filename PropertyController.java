package com.example.project2.controller;

import com.example.project2.model.AppUser;
import com.example.project2.model.Property;
import com.example.project2.model.UserRole;
import com.example.project2.repository.AppUserRepository;
import com.example.project2.repository.PropertyRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class PropertyController {

    private final PropertyRepository propertyRepository;
    private final AppUserRepository userRepository;

    public PropertyController(PropertyRepository propertyRepository, AppUserRepository userRepository) {
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/properties")
    public String list(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "priceMin", required = false) Float priceMin,
            @RequestParam(value = "priceMax", required = false) Float priceMax,
            @RequestParam(value = "areaMin", required = false) Float areaMin,
            @RequestParam(value = "areaMax", required = false) Float areaMax,
            @RequestParam(value = "rooms", required = false) Integer rooms,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "yearBuiltMin", required = false) Integer yearBuiltMin,
            @RequestParam(value = "yearBuiltMax", required = false) Integer yearBuiltMax,
            @RequestParam(value = "buildingMaterial", required = false) String buildingMaterial,
            @RequestParam(value = "floor", required = false) Integer floor,
            @RequestParam(value = "sort", required = false, defaultValue = "relevance") String sort,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "12") int size,
            Model model
    ) {
        List<Property> items = propertyRepository.findLatestActive();
        
        // Применяем фильтры
        if (q != null && !q.isBlank()) {
            String qq = q.toLowerCase();
            items = items.stream().filter(p ->
                    (p.getAddress() != null && p.getAddress().toLowerCase().contains(qq)) ||
                    (p.getCity() != null && p.getCity().toLowerCase().contains(qq)) ||
                    (p.getDistrict() != null && p.getDistrict().toLowerCase().contains(qq))
            ).collect(Collectors.toList());
        }
        if (priceMin != null) {
            items = items.stream().filter(p -> p.getPrice() != null && p.getPrice() >= priceMin).collect(Collectors.toList());
        }
        if (priceMax != null) {
            items = items.stream().filter(p -> p.getPrice() != null && p.getPrice() <= priceMax).collect(Collectors.toList());
        }
        if (areaMin != null || areaMax != null) {
            items = items.stream().filter(p -> {
                if (p.getArea() == null) return false;
                if (areaMin != null && p.getArea() < areaMin) return false;
                if (areaMax != null && p.getArea() > areaMax) return false;
                return true;
            }).collect(Collectors.toList());
        }
        if (rooms != null) {
            items = items.stream().filter(p -> p.getRooms() != null && (rooms >= 4 ? p.getRooms() >= 4 : p.getRooms().equals(rooms))).collect(Collectors.toList());
        }
        if (type != null && !type.isBlank()) {
            items = items.stream().filter(p -> type.equalsIgnoreCase(p.getType())).collect(Collectors.toList());
        }
        if (category != null && !category.isBlank()) {
            items = items.stream().filter(p -> category.equalsIgnoreCase(p.getCategory())).collect(Collectors.toList());
        }
        if (yearBuiltMin != null || yearBuiltMax != null) {
            items = items.stream().filter(p -> {
                if (p.getYearBuilt() == null) return false;
                if (yearBuiltMin != null && p.getYearBuilt() < yearBuiltMin) return false;
                if (yearBuiltMax != null && p.getYearBuilt() > yearBuiltMax) return false;
                return true;
            }).collect(Collectors.toList());
        }
        if (buildingMaterial != null && !buildingMaterial.isBlank()) {
            items = items.stream().filter(p -> p.getBuildingMaterial() != null && buildingMaterial.equalsIgnoreCase(p.getBuildingMaterial())).collect(Collectors.toList());
        }
        if (floor != null) {
            items = items.stream().filter(p -> p.getFloor() != null && p.getFloor().equals(floor)).collect(Collectors.toList());
        }
        
        // Применяем сортировку
        items = applySorting(items, sort);
        
        // Пагинация (с защитой от выхода за границы)
        int totalItems = items.size();
        int safeSize = Math.max(1, size);
        int totalPages = (int) Math.ceil(totalItems / (double) safeSize);
        int safePage = Math.max(0, Math.min(page, Math.max(0, totalPages - 1)));
        int start = safePage * safeSize;
        int end = Math.min(start + safeSize, Math.max(start, totalItems));
        List<Property> pagedItems = (start <= end) ? items.subList(start, end) : java.util.Collections.emptyList();
        
        model.addAttribute("items", pagedItems);
        model.addAttribute("q", q);
        model.addAttribute("priceMin", priceMin);
        model.addAttribute("priceMax", priceMax);
        model.addAttribute("areaMin", areaMin);
        model.addAttribute("areaMax", areaMax);
        model.addAttribute("rooms", rooms);
        model.addAttribute("type", type);
        model.addAttribute("category", category);
        model.addAttribute("yearBuiltMin", yearBuiltMin);
        model.addAttribute("yearBuiltMax", yearBuiltMax);
        model.addAttribute("buildingMaterial", buildingMaterial);
        model.addAttribute("floor", floor);
        model.addAttribute("sort", sort);
        model.addAttribute("currentPage", safePage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("pageSize", safeSize);
        
        // Добавляем email текущего пользователя и информацию о роли для JavaScript и шаблонов
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            String email = auth.getName();
            model.addAttribute("currentUserEmail", email);
            
            // Проверяем, является ли пользователь администратором
            Optional<AppUser> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                AppUser user = userOpt.get();
                model.addAttribute("isCurrentUserAdmin", user.getRole() == UserRole.ADMIN);
            } else {
                model.addAttribute("isCurrentUserAdmin", false);
            }
        } else {
            model.addAttribute("isCurrentUserAdmin", false);
        }
        
        return "property/list";
    }
    
    private List<Property> applySorting(List<Property> items, String sort) {
        return items.stream().sorted((p1, p2) -> {
            switch (sort) {
                case "price_asc":
                    return comparePrices(p1, p2, true);
                case "price_desc":
                    return comparePrices(p1, p2, false);
                case "area_asc":
                    return compareAreas(p1, p2, true);
                case "area_desc":
                    return compareAreas(p1, p2, false);
                case "date_desc":
                    return compareDates(p1, p2, false);
                case "date_asc":
                    return compareDates(p1, p2, true);
                case "relevance":
                default:
                    // Сначала продвинутые, затем по дате
                    if (p1.getPromoted() != null && p1.getPromoted() && (p2.getPromoted() == null || !p2.getPromoted())) {
                        return -1;
                    }
                    if (p2.getPromoted() != null && p2.getPromoted() && (p1.getPromoted() == null || !p1.getPromoted())) {
                        return 1;
                    }
                    return compareDates(p1, p2, false);
            }
        }).collect(Collectors.toList());
    }
    
    private int comparePrices(Property p1, Property p2, boolean ascending) {
        Float price1 = p1.getPrice() != null ? p1.getPrice() : 0f;
        Float price2 = p2.getPrice() != null ? p2.getPrice() : 0f;
        // При сортировке по цене не учитываем продвижение - сортируем строго по цене
        int result = price1.compareTo(price2);
        return ascending ? result : -result;
    }
    
    private int compareAreas(Property p1, Property p2, boolean ascending) {
        Float area1 = p1.getArea() != null ? p1.getArea() : 0f;
        Float area2 = p2.getArea() != null ? p2.getArea() : 0f;
        // При сортировке по площади не учитываем продвижение - сортируем строго по площади
        int result = area1.compareTo(area2);
        return ascending ? result : -result;
    }
    
    private int compareDates(Property p1, Property p2, boolean ascending) {
        // При сортировке по дате не учитываем продвижение - сортируем строго по дате
        LocalDateTime date1 = p1.getCreatedDate() != null ? p1.getCreatedDate() : java.time.LocalDateTime.MIN;
        LocalDateTime date2 = p2.getCreatedDate() != null ? p2.getCreatedDate() : java.time.LocalDateTime.MIN;
        int result = date1.compareTo(date2);
        return ascending ? result : -result;
    }

    @GetMapping("/properties/{id}")
    public String details(
            @PathVariable("id") Long id,
            @RequestParam(value = "from", required = false) String from,
            Model model) {
        Optional<Property> propertyOpt = propertyRepository.findById(id);
        if (propertyOpt.isEmpty()) {
            model.addAttribute("notFound", true);
            model.addAttribute("id", id);
            return "property/detail";
        }
        Property property = propertyOpt.get();
        // Загружаем связи user и realtor, если они еще не загружены
        if (property.getUser() != null) {
            property.getUser().getEmail(); // инициализируем lazy loading
        }
        if (property.getRealtor() != null) {
            property.getRealtor().getEmail(); // инициализируем lazy loading
        }
        
        // Проверяем, является ли текущий пользователь владельцем объявления
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isOwner = false;
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            String email = auth.getName();
            if (property.getUser() != null && property.getUser().getEmail().equals(email)) {
                isOwner = true;
            }
        }
        
        model.addAttribute("p", property);
        model.addAttribute("from", from);
        model.addAttribute("isOwner", isOwner);
        return "property/detail";
    }
}
