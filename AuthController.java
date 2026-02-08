package com.example.project2.controller;

import com.example.project2.model.AppUser;
import com.example.project2.model.UserRole;
import com.example.project2.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AppUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new AppUser());
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@ModelAttribute("user") AppUser user, Model model) {
        try {
            System.out.println("=== Простая регистрация ===");
            System.out.println("Email: " + user.getEmail());
            System.out.println("Имя: " + user.getFirstName());
            System.out.println("Фамилия: " + user.getLastName());
            System.out.println("Роль: " + user.getRole());
            
            // Простые проверки
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                model.addAttribute("error", "❌ Email обязателен");
                return "register";
            }
            
            if (user.getPasswordHash() == null || user.getPasswordHash().trim().isEmpty()) {
                model.addAttribute("error", "❌ Пароль обязателен");
                return "register";
            }
            
            if (user.getFirstName() == null || user.getFirstName().trim().isEmpty()) {
                model.addAttribute("error", "❌ Имя обязательно");
                return "register";
            }
            
            if (user.getLastName() == null || user.getLastName().trim().isEmpty()) {
                model.addAttribute("error", "❌ Фамилия обязательна");
                return "register";
            }
            
            // Установка роли по умолчанию
            if (user.getRole() == null) {
                user.setRole(UserRole.USER);
            }
            
            // Запрет регистрации администратора
            if (user.getRole() == UserRole.ADMIN) {
                model.addAttribute("error", "❌ Регистрация администратора запрещена");
                return "register";
            }
            
            // Проверка уникальности email
            if (repository.existsByEmail(user.getEmail())) {
                model.addAttribute("error", "❌ Пользователь с таким email уже существует");
                return "register";
            }
            
            // Сохранение
            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
            repository.save(user);
            System.out.println("✅ Пользователь сохранен: " + user.getEmail());
            return "redirect:/login?registered=true&email=" + user.getEmail();
            
        } catch (Exception e) {
            System.err.println("Ошибка регистрации: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Ошибка: " + e.getMessage());
            return "register";
        }
    }
}


