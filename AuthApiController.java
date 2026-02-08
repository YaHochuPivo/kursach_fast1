package com.example.project2.controller;

import com.example.project2.model.AppUser;
import com.example.project2.model.UserRole;
import com.example.project2.repository.AppUserRepository;
import com.example.project2.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/api/auth")
@Tag(name = "Authentication API", description = "API для регистрации и авторизации пользователей")
public class AuthApiController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthApiController(AppUserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    @Operation(summary = "Авторизация пользователя", description = "Авторизует пользователя и возвращает JWT токен")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        Optional<AppUser> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPasswordHash())) {
            response.put("success", false);
            response.put("message", "Неверный email или пароль");
            return ResponseEntity.badRequest().body(response);
        }
        
        AppUser user = userOpt.get();
        String token = jwtService.generateToken(user.getEmail());
        
        response.put("success", true);
        response.put("token", token);
        response.put("user", Map.of(
            "id", user.getId(),
            "email", user.getEmail(),
            "firstName", user.getFirstName(),
            "lastName", user.getLastName(),
            "role", user.getRole().name(),
            "phone", user.getPhone(),
            "realtorLicense", user.getRealtorLicense()
        ));
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "Простая регистрация", description = "Простая регистрация пользователя")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("=== Простая API регистрация ===");
            System.out.println("Email: " + request.getEmail());
            
            // Простые проверки
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Email обязателен");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Пароль обязателен");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Проверка уникальности email
            if (userRepository.existsByEmail(request.getEmail())) {
                response.put("success", false);
                response.put("message", "Пользователь с таким email уже существует");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Создание пользователя
            AppUser user = new AppUser();
            user.setEmail(request.getEmail());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setFirstName(request.getFirstName() != null ? request.getFirstName() : "Пользователь");
            user.setLastName(request.getLastName() != null ? request.getLastName() : "Тест");
            user.setPhone(request.getPhone());
            user.setRole(request.getRole() != null ? request.getRole() : UserRole.USER);
            user.setRealtorLicense(request.getRealtorLicense());
            
            AppUser savedUser = userRepository.save(user);
            String token = jwtService.generateToken(savedUser.getEmail());
            
            response.put("success", true);
            response.put("message", "Пользователь успешно зарегистрирован");
            response.put("token", token);
            response.put("user", Map.of(
                "id", savedUser.getId(),
                "email", savedUser.getEmail(),
                "firstName", savedUser.getFirstName(),
                "lastName", savedUser.getLastName(),
                "role", savedUser.getRole().name()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("Ошибка API регистрации: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Внутренние классы для запросов
    public static class LoginRequest {
        private String email;
        private String password;
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    public static class RegisterRequest {
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        private String phone;
        private UserRole role;
        private String realtorLicense;
        
        // Геттеры и сеттеры
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public UserRole getRole() { return role; }
        public void setRole(UserRole role) { this.role = role; }
        public String getRealtorLicense() { return realtorLicense; }
        public void setRealtorLicense(String realtorLicense) { this.realtorLicense = realtorLicense; }
    }
}