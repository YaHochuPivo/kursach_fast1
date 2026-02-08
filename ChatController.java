package com.example.project2.controller;

import com.example.project2.model.AppUser;
import com.example.project2.model.Chat;
import com.example.project2.model.Property;
import com.example.project2.repository.AppUserRepository;
import com.example.project2.repository.ChatRepository;
import com.example.project2.repository.MessageRepository;
import com.example.project2.repository.PropertyRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
public class ChatController {

    private final PropertyRepository propertyRepository;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final AppUserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    public ChatController(PropertyRepository propertyRepository, ChatRepository chatRepository, MessageRepository messageRepository, AppUserRepository userRepository, JdbcTemplate jdbcTemplate) {
        this.propertyRepository = propertyRepository;
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam("propertyId") Long propertyId, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Optional<AppUser> currentUserOpt = userRepository.findByEmail(email);
        
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        
        AppUser currentUser = currentUserOpt.get();
        // Используем метод с JOIN FETCH для загрузки user и realtor
        Optional<Property> propertyOpt = propertyRepository.findByIdWithUser(propertyId);
        
        if (propertyOpt.isEmpty()) {
            model.addAttribute("error", "Объявление не найдено");
            return "error";
        }
        
        Property property = propertyOpt.get();
        
        // Проверяем, является ли текущий пользователь автором объявления
        boolean isOwner = property.getUser() != null && property.getUser().getId().equals(currentUser.getId());
        
        // Если пользователь - автор, проверяем, есть ли уже чат для этого объявления с его участием
        if (isOwner) {
            // Ищем все чаты для этого объявления, где пользователь является продавцом (seller)
            // Автор объявления всегда seller в чате
            List<Chat> existingChats = chatRepository.findByPropertyId(propertyId);
            Chat existingChat = existingChats.stream()
                    .filter(c -> {
                        // Проверяем, что seller существует и это текущий пользователь
                        if (c.getSeller() != null && c.getSeller().getId().equals(currentUser.getId())) {
                            return true;
                        }
                        // Также проверяем buyer на случай, если структура данных изменилась
                        if (c.getBuyer() != null && c.getBuyer().getId().equals(currentUser.getId())) {
                            return true;
                        }
                        return false;
                    })
                    .findFirst()
                    .orElse(null);
            
            if (existingChat != null) {
                // Если чат уже существует, открываем его
                Chat chat = existingChat;
                
                // JOIN FETCH уже загрузил buyer, seller и property, но для безопасности инициализируем
                if (chat.getBuyer() != null) {
                    chat.getBuyer().getEmail();
                    chat.getBuyer().getFirstName();
                    chat.getBuyer().getLastName();
                }
                if (chat.getSeller() != null) {
                    chat.getSeller().getEmail();
                    chat.getSeller().getFirstName();
                    chat.getSeller().getLastName();
                }
                if (chat.getProperty() != null) {
                    chat.getProperty().getAddress();
                }
                
                // Загружаем сообщения
                List<com.example.project2.model.Message> messages = messageRepository.findByChatIdOrderBySentDate(chat.getId());
                messages.forEach(msg -> {
                    if (msg.getUser() != null) {
                        msg.getUser().getEmail();
                        msg.getUser().getFirstName();
                        msg.getUser().getLastName();
                    }
                });
                
                // Определяем собеседника (для автора объявления это всегда buyer)
                AppUser otherUser = null;
                if (chat.getSeller() != null && chat.getSeller().getId().equals(currentUser.getId())) {
                    // Текущий пользователь - продавец, собеседник - покупатель
                    otherUser = chat.getBuyer();
                } else if (chat.getBuyer() != null && chat.getBuyer().getId().equals(currentUser.getId())) {
                    // Текущий пользователь - покупатель, собеседник - продавец
                    otherUser = chat.getSeller();
                }
                
                if (otherUser == null) {
                    model.addAttribute("error", "Не удалось определить собеседника");
                    return "error";
                }
                
                model.addAttribute("chat", chat);
                model.addAttribute("property", property);
                model.addAttribute("messages", messages);
                model.addAttribute("currentUser", currentUser);
                model.addAttribute("otherUser", otherUser);
                
                return "chat";
            } else {
                // Если пользователь - автор, но чата еще нет, не разрешаем создавать новый
                model.addAttribute("error", "Вы не можете написать самому себе. Дождитесь, пока кто-то напишет вам по вашему объявлению.");
                return "error";
            }
        }
        
        // Если пользователь не является автором, создаем или находим чат как обычно
        AppUser buyer = currentUser;
        AppUser seller = property.getUser();
        
        if (seller == null) {
            model.addAttribute("error", "Автор объявления не найден");
            return "error";
        }
        
        // Ищем или создаем чат
        Chat chat = chatRepository.findByPropertyAndUsers(propertyId, buyer.getId(), seller.getId());
        boolean isNewChat = false;
        if (chat == null) {
            chat = new Chat();
            chat.setProperty(property);
            chat.setBuyer(buyer);
            chat.setSeller(seller);
            chat = chatRepository.save(chat);
            isNewChat = true;
        }
        
        // Загружаем сообщения
        List<com.example.project2.model.Message> messages = messageRepository.findByChatIdOrderBySentDate(chat.getId());
        
        // Если чат новый и пустой, сохраняем информацию для проверки при выходе
        if (isNewChat && messages.isEmpty()) {
            model.addAttribute("isNewEmptyChat", true);
            model.addAttribute("chatId", chat.getId());
        }
        
        // Инициализируем lazy loading для user в сообщениях
        messages.forEach(msg -> {
            if (msg.getUser() != null) {
                msg.getUser().getEmail();
                msg.getUser().getFirstName();
                msg.getUser().getLastName();
            }
        });
        
        // Инициализируем lazy loading для buyer и seller
        if (chat.getBuyer() != null) {
            chat.getBuyer().getEmail();
            chat.getBuyer().getFirstName();
            chat.getBuyer().getLastName();
        }
        if (chat.getSeller() != null) {
            chat.getSeller().getEmail();
            chat.getSeller().getFirstName();
            chat.getSeller().getLastName();
        }
        
        // Инициализируем lazy loading для property
        if (chat.getProperty() != null) {
            chat.getProperty().getAddress();
        }
        
        // Определяем собеседника
        AppUser otherUser = currentUser.getId().equals(buyer.getId()) ? seller : buyer;
        
        // Отмечаем чат как прочитанный при открытии
        markChatAsRead(chat.getId(), currentUser.getId());
        
        model.addAttribute("chat", chat);
        model.addAttribute("property", property);
        model.addAttribute("messages", messages);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("otherUser", otherUser);
        
        return "chat";
    }
    
    @Transactional
    private void markChatAsRead(Long chatId, Long userId) {
        try {
            // Сначала убеждаемся, что таблица существует
            ensureChatReadsTableExists();
            
            LocalDateTime now = LocalDateTime.now();
            // Сначала пробуем обновить существующую запись
            int updated = jdbcTemplate.update(
                "UPDATE chat_reads SET last_read_date = ? WHERE chat_id = ? AND user_id = ?",
                now, chatId, userId
            );
            
            // Если запись не найдена, создаем новую
            if (updated == 0) {
                try {
                    jdbcTemplate.update(
                        "INSERT INTO chat_reads (chat_id, user_id, last_read_date) VALUES (?, ?, ?)",
                        chatId, userId, now
                    );
                    System.out.println("✓ [ChatController] Чат " + chatId + " отмечен как прочитанный для пользователя " + userId + " (создана новая запись)");
                } catch (Exception insertEx) {
                    // Если INSERT не удался, пробуем UPDATE еще раз
                    updated = jdbcTemplate.update(
                        "UPDATE chat_reads SET last_read_date = ? WHERE chat_id = ? AND user_id = ?",
                        now, chatId, userId
                    );
                    System.out.println("✓ [ChatController] Чат " + chatId + " отмечен как прочитанный для пользователя " + userId + " (обновлена после INSERT конфликта, строк: " + updated + ")");
                }
            } else {
                System.out.println("✓ [ChatController] Чат " + chatId + " отмечен как прочитанный для пользователя " + userId + " (обновлена существующая запись)");
            }
            
            // Проверяем, что запись действительно сохранена
            try {
                List<LocalDateTime> results = jdbcTemplate.query(
                    "SELECT last_read_date FROM chat_reads WHERE chat_id = ? AND user_id = ?",
                    (rs, rowNum) -> rs.getTimestamp("last_read_date").toLocalDateTime(),
                    chatId, userId
                );
                if (results.isEmpty()) {
                    System.err.println("⚠ [ChatController] ВНИМАНИЕ: Запись не найдена сразу после сохранения! chatId=" + chatId + ", userId=" + userId);
                } else {
                    System.out.println("✓ [ChatController] Подтверждено: last_read_date сохранен как " + results.get(0));
                }
            } catch (Exception checkEx) {
                System.err.println("⚠ [ChatController] Ошибка при проверке сохранения: " + checkEx.getMessage());
            }
        } catch (org.springframework.jdbc.BadSqlGrammarException e) {
            System.err.println("✗ [ChatController] SQL ошибка при отметке чата как прочитанного (таблица не существует): " + e.getMessage());
            // Пытаемся создать таблицу
            try {
                ensureChatReadsTableExists();
                // Повторяем попытку после создания таблицы
                markChatAsRead(chatId, userId);
            } catch (Exception retryEx) {
                System.err.println("✗ [ChatController] Не удалось создать таблицу или повторить операцию: " + retryEx.getMessage());
            }
        } catch (Exception e) {
            System.err.println("✗ [ChatController] Ошибка при отметке чата как прочитанного: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void ensureChatReadsTableExists() {
        try {
            // Проверяем, существует ли таблица
            jdbcTemplate.queryForObject("SELECT 1 FROM chat_reads LIMIT 1", Integer.class);
        } catch (Exception e) {
            // Таблица не существует, создаем её
            System.out.println("⚠ [ChatController] Таблица chat_reads не существует, создаем...");
            try {
                jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS chat_reads (
                        id BIGSERIAL PRIMARY KEY,
                        chat_id BIGINT NOT NULL,
                        user_id BIGINT NOT NULL,
                        last_read_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(chat_id, user_id),
                        FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE,
                        FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
                    )
                    """);
                // Создаем уникальный индекс
                try {
                    jdbcTemplate.execute("""
                        CREATE UNIQUE INDEX IF NOT EXISTS chat_reads_chat_user_unique 
                        ON chat_reads(chat_id, user_id)
                        """);
                } catch (Exception indexEx) {
                    System.out.println("⚠ [ChatController] Не удалось создать уникальный индекс (возможно, уже существует): " + indexEx.getMessage());
                }
                System.out.println("✓ [ChatController] Таблица chat_reads создана");
            } catch (Exception createEx) {
                System.err.println("✗ [ChatController] Не удалось создать таблицу chat_reads: " + createEx.getMessage());
                createEx.printStackTrace();
                throw createEx;
            }
        }
    }
    
    @GetMapping("/chats")
    public String chatsList(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Optional<AppUser> currentUserOpt = userRepository.findByEmail(email);
        
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        
        model.addAttribute("currentUser", currentUserOpt.get());
        return "chats/list";
    }
}

