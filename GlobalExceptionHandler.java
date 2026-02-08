package com.example.project2.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Ошибка валидации",
                LocalDateTime.now(),
                request.getDescription(false),
                errors
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public Object handleGlobalException(Exception ex, WebRequest request, Model model) {
        // Логируем ошибку для отладки
        System.err.println("Ошибка в приложении: " + ex.getMessage());
        ex.printStackTrace();
        
        try {
            // Проверяем, является ли это запросом для веб-страницы или API
            String path = request.getDescription(false);
            if (path == null) {
                path = "";
            }
            path = path.replace("uri=", "");
            
            if (path.startsWith("/v1/api/")) {
                // Это API запрос - возвращаем JSON
                ErrorResponse errorResponse = new ErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Внутренняя ошибка сервера: " + ex.getMessage(),
                        LocalDateTime.now(),
                        path
                );
                return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
            } else {
                // Это веб-запрос - возвращаем страницу ошибки или главную страницу
                try {
                    model.addAttribute("error", "Произошла ошибка. Пожалуйста, попробуйте снова.");
                    return "error";
                } catch (Exception e) {
                    // Если даже страница ошибки не загружается, возвращаем редирект на главную
                    System.err.println("Не удалось загрузить страницу ошибки: " + e.getMessage());
                    return "redirect:/";
                }
            }
        } catch (Exception e) {
            System.err.println("Критическая ошибка в обработчике исключений: " + e.getMessage());
            e.printStackTrace();
            // В крайнем случае возвращаем редирект
            return "redirect:/";
        }
    }

    public static class ErrorResponse {
        private int status;
        private String message;
        private LocalDateTime timestamp;
        private String path;
        private Map<String, String> validationErrors;

        public ErrorResponse(int status, String message, LocalDateTime timestamp, String path) {
            this.status = status;
            this.message = message;
            this.timestamp = timestamp;
            this.path = path;
        }

        public ErrorResponse(int status, String message, LocalDateTime timestamp, String path, Map<String, String> validationErrors) {
            this.status = status;
            this.message = message;
            this.timestamp = timestamp;
            this.path = path;
            this.validationErrors = validationErrors;
        }

        // Getters and setters
        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public Map<String, String> getValidationErrors() { return validationErrors; }
        public void setValidationErrors(Map<String, String> validationErrors) { this.validationErrors = validationErrors; }
    }
}
