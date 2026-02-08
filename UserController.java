package com.example.project2.controller;

import com.example.project2.model.AppUser;
import com.example.project2.model.Favorite;
import com.example.project2.model.Property;
import com.example.project2.repository.AppUserRepository;
import com.example.project2.repository.FavoriteRepository;
import com.example.project2.repository.PropertyRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class UserController {

    private final AppUserRepository userRepository;
    private final FavoriteRepository favoriteRepository;
    private final PropertyRepository propertyRepository;

    public UserController(AppUserRepository userRepository, FavoriteRepository favoriteRepository, PropertyRepository propertyRepository) {
        this.userRepository = userRepository;
        this.favoriteRepository = favoriteRepository;
        this.propertyRepository = propertyRepository;
    }

    @GetMapping("/user/dashboard")
    public String userDashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("username", auth.getName());
        return "user/dashboard";
    }

    @GetMapping("/user/properties")
    public String userProperties(Model model, @org.springframework.web.bind.annotation.RequestParam(value = "status", required = false) String status) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        
        // Передаем email текущего пользователя для проверки авторства
        model.addAttribute("currentUserEmail", email);
        
        if (userOpt.isPresent()) {
            AppUser user = userOpt.get();
            List<Property> userProperties = propertyRepository.findByUserId(user.getId());
            if (status != null && status.equalsIgnoreCase("archived")) {
                // Показываем только архивные во вкладке "Архив"
                userProperties = userProperties.stream()
                        .filter(p -> p.getStatus() != null && p.getStatus().name().equalsIgnoreCase("archived"))
                        .collect(Collectors.toList());
                model.addAttribute("tabArchived", true);
            } else {
                // Во вкладке "Активные" скрываем архивные
                userProperties = userProperties.stream()
                        .filter(p -> p.getStatus() == null || !p.getStatus().name().equalsIgnoreCase("archived"))
                        .collect(Collectors.toList());
                model.addAttribute("tabArchived", false);
            }
            model.addAttribute("items", userProperties);
            model.addAttribute("user", user);
        } else {
            model.addAttribute("items", List.of());
        }
        
        return "user/properties";
    }

    @GetMapping("/user/favorites")
    public String userFavorites(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Проверяем, является ли пользователь неавторизованным
        boolean isAnonymous = auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser");
        model.addAttribute("isAnonymous", isAnonymous);
        
        if (!isAnonymous) {
            String email = auth.getName();
            Optional<AppUser> userOpt = userRepository.findByEmail(email);
            
            if (userOpt.isPresent()) {
                AppUser user = userOpt.get();
                // Показываем избранное только если пользователь не администратор
                if (user.getRole() == null || !user.getRole().name().equals("ADMIN")) {
                    List<Favorite> favorites = favoriteRepository.findByUser(user);
                    // Загружаем свойства с user и realtor для отображения автора
                    List<Property> properties = favorites.stream()
                            .map(Favorite::getProperty)
                            .collect(Collectors.toList());
                    // Инициализируем lazy loading для user и realtor
                    properties.forEach(p -> {
                        if (p.getUser() != null) p.getUser().getEmail();
                        if (p.getRealtor() != null) p.getRealtor().getEmail();
                    });
                    model.addAttribute("favorites", favorites);
                    model.addAttribute("items", properties);
                } else {
                    // Для администратора показываем пустой список
                    model.addAttribute("items", List.of());
                }
            } else {
                model.addAttribute("items", List.of());
            }
        } else {
            // Для неавторизованных пользователей показываем пустой список
            model.addAttribute("items", List.of());
        }
        
        return "user/favorites";
    }

    @GetMapping("/user/profile")
    public String userProfile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            model.addAttribute("user", userOpt.get());
        }
        return "user/profile";
    }
}
