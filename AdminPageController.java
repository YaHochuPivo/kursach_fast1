package com.example.project2.controller;

import com.example.project2.model.UserRole;
import com.example.project2.repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPageController {

    private final AppUserRepository userRepository;

    public AdminPageController(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/admin/logs")
    public String logs(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth != null ? auth.getName() : null;
        boolean isAdmin = email != null && userRepository.findByEmail(email)
                .map(u -> u.getRole() == UserRole.ADMIN)
                .orElse(false);
        if (!isAdmin) {
            model.addAttribute("error", "Доступ запрещён");
            return "error";
        }
        model.addAttribute("currentUserEmail", email);
        return "admin/logs";
    }
}
