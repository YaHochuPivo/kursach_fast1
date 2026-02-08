package com.example.project2.controller;

import com.example.project2.model.AppUser;
import com.example.project2.repository.AppUserRepository;
import com.example.project2.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/api/user")
public class UserApiController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserApiController(AppUserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");
        
        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Текущий пароль обязателен");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (newPassword == null || newPassword.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Новый пароль обязателен");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (newPassword.length() < 3) {
            response.put("success", false);
            response.put("message", "Пароль должен содержать минимум 3 символа");
            return ResponseEntity.badRequest().body(response);
        }
        
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Пользователь не найден");
            return ResponseEntity.badRequest().body(response);
        }
        
        AppUser user = userOpt.get();
        
        // Проверяем текущий пароль
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            response.put("success", false);
            response.put("message", "Неверный текущий пароль");
            return ResponseEntity.badRequest().body(response);
        }
        
        // Устанавливаем новый пароль
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        response.put("success", true);
        response.put("message", "Пароль успешно изменен");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/update-phone")
    public ResponseEntity<Map<String, Object>> updatePhone(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        String phone = request.get("phone");
        
        if (phone == null || phone.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Номер телефона обязателен");
            return ResponseEntity.badRequest().body(response);
        }
        
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Пользователь не найден");
            return ResponseEntity.badRequest().body(response);
        }
        
        AppUser user = userOpt.get();
        user.setPhone(phone);
        userRepository.save(user);
        
        response.put("success", true);
        response.put("message", "Телефон успешно обновлен");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        String email = request.get("email");
        
        if (email == null || email.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Email обязателен");
            return ResponseEntity.badRequest().body(response);
        }
        
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        // Не раскрываем, существует ли email
        if (userOpt.isPresent()) {
            AppUser user = userOpt.get();
            // Генерируем новый пароль
            String newPassword = generatePassword(10);
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            // Отправляем письмо
            String subject = "Восстановление пароля — RealEstate";
            String fullName = (user.getFirstName() != null ? user.getFirstName() : "")
                    + (user.getLastName() != null && !user.getLastName().isBlank() ? " " + user.getLastName() : "");
            String displayName = fullName.isBlank() ? user.getEmail() : fullName;
            String html = "" +
                "<div style=\"font-family:Arial,Helvetica,sans-serif; color:#0f172a;\">" +
                "  <div style=\"max-width:560px;margin:0 auto;border:1px solid #e2e8f0;border-radius:8px;overflow:hidden\">" +
                "    <div style=\"padding:14px 16px;background:#eff6ff;border-bottom:1px solid #dbeafe;font-weight:700;color:#1e293b\">" +
                "      Восстановление пароля — RealEstate" +
                "    </div>" +
                "    <div style=\"padding:16px\">" +
                "      <p style=\"margin:0 0 12px;\">Здравствуйте, " + escapeHtml(displayName) + "!</p>" +
                "      <p style=\"margin:0 0 12px;\">Ваш новый пароль:</p>" +
                "      <div style=\"margin:8px 0 16px; padding:12px; background:#0f172a; color:#fff; border-radius:6px; font-family:ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace; font-size:16px; letter-spacing:0.5px; display:inline-block;\">" +
                "        " + escapeHtml(newPassword) +
                "      </div>" +
                "      <p style=\"margin:0 0 12px;\">Рекомендуем <strong>сразу сменить пароль</strong> в вашем профиле после входа.</p>" +
                "      <p style=\"margin:0; color:#64748b;\">Если вы не запрашивали восстановление — просто проигнорируйте это письмо.</p>" +
                "    </div>" +
                "  </div>" +
                "</div>";
            emailService.sendHtml(email, subject, html);
        }
        response.put("success", true);
        response.put("message", "Если email существует, новый пароль отправлен на почту");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/update-passport")
    public ResponseEntity<Map<String, Object>> updatePassport(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Пользователь не найден");
            return ResponseEntity.badRequest().body(response);
        }
        AppUser user = userOpt.get();
        // Разрешаем хранить паспортные данные для обычных пользователей и риелторов (запрещаем админам)
        if (user.getRole() != null && user.getRole().name().equals("ADMIN")) {
            response.put("success", false);
            response.put("message", "Изменение недоступно для администратора");
            return ResponseEntity.badRequest().body(response);
        }

        // Поддержка старого поля на случай обратной совместимости
        String passport = request.getOrDefault("passportData", null);
        if (passport != null && passport.trim().isEmpty()) passport = null;
        user.setPassportData(passport);

        // Новые поля: серия (4 цифры) и номер (6 цифр). Поля необязательные.
        String series = request.getOrDefault("passportSeries", null);
        String number = request.getOrDefault("passportNumber", null);

        if (series != null) series = series.trim();
        if (number != null) number = number.trim();

        // Пустые строки трактуем как очистку
        if (series != null && series.isEmpty()) series = null;
        if (number != null && number.isEmpty()) number = null;

        // Валидация формата, если значение передано
        if (series != null && !series.matches("\\d{4}")) {
            response.put("success", false);
            response.put("message", "Серия паспорта должна состоять из 4 цифр");
            return ResponseEntity.badRequest().body(response);
        }
        if (number != null && !number.matches("\\d{6}")) {
            response.put("success", false);
            response.put("message", "Номер паспорта должен состоять из 6 цифр");
            return ResponseEntity.badRequest().body(response);
        }

        user.setPassportSeries(series);
        user.setPassportNumber(number);

        userRepository.save(user);
        response.put("success", true);
        response.put("message", "Паспортные данные сохранены");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/update-service-fee")
    public ResponseEntity<Map<String, Object>> updateServiceFee(@RequestBody Map<String, String> payload) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return ResponseEntity.status(401).build();
        Optional<AppUser> userOpt = userRepository.findByEmail(auth.getName());
        if (userOpt.isEmpty()) return ResponseEntity.status(401).build();
        AppUser user = userOpt.get();
        if (user.getRole() == null || !(user.getRole().name().equals("REALTOR") || user.getRole().name().equals("ADMIN"))) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Доступно только для риелтора"));
        }
        String feeStr = payload.getOrDefault("serviceFee", "").trim().replace(" ", "");
        if (feeStr.isEmpty()) {
            user.setServiceFee(null);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("success", true));
        }
        try {
            java.math.BigDecimal fee = new java.math.BigDecimal(feeStr);
            if (fee.compareTo(java.math.BigDecimal.ZERO) < 0) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Сумма не может быть отрицательной"));
            }
            if (fee.compareTo(new java.math.BigDecimal("1000000000")) > 0) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Слишком большая сумма"));
            }
            user.setServiceFee(fee);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Неверный формат суммы"));
        }
    }

    private String generatePassword(int len) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            int idx = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(idx));
        }
        return sb.toString();
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

