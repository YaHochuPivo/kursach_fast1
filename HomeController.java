package com.example.project2.controller;

import com.example.project2.model.AppUser;
import com.example.project2.model.Property;
import com.example.project2.model.PropertyViewHistory;
import com.example.project2.model.UserRole;
import com.example.project2.repository.AppUserRepository;
import com.example.project2.repository.PropertyRepository;
import com.example.project2.repository.PropertyViewHistoryRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final PropertyRepository propertyRepository;
    private final AppUserRepository userRepository;
    private final PropertyViewHistoryRepository viewHistoryRepository;

    public HomeController(PropertyRepository propertyRepository, AppUserRepository userRepository, PropertyViewHistoryRepository viewHistoryRepository) {
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.viewHistoryRepository = viewHistoryRepository;
    }

    @GetMapping({"/", "/index"})
    public String index(
            @org.springframework.web.bind.annotation.RequestParam(value = "edit", required = false) Long editId,
            Model model) {
        try {
            List<com.example.project2.model.Property> popular = new ArrayList<>();
            List<com.example.project2.model.Property> latest = new ArrayList<>();
            List<com.example.project2.model.Property> affordable = new ArrayList<>();
            
            try {
                popular = propertyRepository.findPopularActive().stream().limit(12).toList();
                // Инициализируем lazy loading для всех объявлений (если нужно)
                // LEFT JOIN FETCH уже загружает user и realtor, поэтому дополнительная инициализация не нужна
            } catch (Exception e) {
                System.err.println("Ошибка при загрузке популярных объявлений: " + e.getMessage());
                e.printStackTrace();
                popular = new ArrayList<>(); // Убеждаемся, что список не null
            }
            
            try {
                latest = propertyRepository.findLatestActive().stream().limit(12).toList();
            } catch (Exception e) {
                System.err.println("Ошибка при загрузке новинок: " + e.getMessage());
                e.printStackTrace();
                latest = new ArrayList<>(); // Убеждаемся, что список не null
            }
            
            try {
                affordable = propertyRepository.findAffordableActive().stream().limit(12).toList();
            } catch (Exception e) {
                System.err.println("Ошибка при загрузке доступных объявлений: " + e.getMessage());
                e.printStackTrace();
                affordable = new ArrayList<>(); // Убеждаемся, что список не null
            }
            
            model.addAttribute("popular", popular);
            model.addAttribute("latest", latest);
            model.addAttribute("affordable", affordable);
            
            // Передаем ID для редактирования, если указан
            if (editId != null) {
                model.addAttribute("editId", editId);
            }
            
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
                    
                    // Загружаем историю просмотров для авторизованных пользователей
                    try {
                        Pageable pageable = PageRequest.of(0, 20);
                        List<PropertyViewHistory> recentViews = viewHistoryRepository.findRecentByUserId(user.getId(), pageable);
                        // Используем LinkedHashSet для сохранения порядка и удаления дубликатов
                        List<Property> recentViewedProperties = recentViews.stream()
                                .map(PropertyViewHistory::getProperty)
                                .filter(p -> p != null)
                                .collect(Collectors.toMap(
                                    Property::getId,
                                    p -> p,
                                    (p1, p2) -> p1,
                                    java.util.LinkedHashMap::new
                                ))
                                .values()
                                .stream()
                                .limit(12)
                                .collect(Collectors.toList());
                        model.addAttribute("recentViewed", recentViewedProperties);
                    } catch (Exception e) {
                        System.err.println("Ошибка при загрузке истории просмотров: " + e.getMessage());
                        e.printStackTrace();
                        model.addAttribute("recentViewed", new ArrayList<>());
                    }
                } else {
                    model.addAttribute("isCurrentUserAdmin", false);
                    model.addAttribute("recentViewed", new ArrayList<>());
                }
            } else {
                model.addAttribute("isCurrentUserAdmin", false);
                model.addAttribute("recentViewed", new ArrayList<>());
            }
            
            return "index";
        } catch (Exception e) {
            System.err.println("Критическая ошибка в HomeController: " + e.getMessage());
            e.printStackTrace();
            // Всегда возвращаем index.html, даже при ошибке
            model.addAttribute("popular", new ArrayList<>());
            model.addAttribute("latest", new ArrayList<>());
            model.addAttribute("affordable", new ArrayList<>());
            return "index";
        }
    }
}
