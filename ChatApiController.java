package com.example.project2.controller;

import com.example.project2.dto.MessageResponse;
import com.example.project2.model.*;
import com.example.project2.repository.AppUserRepository;
import com.example.project2.repository.ChatRepository;
import com.example.project2.repository.MessageRepository;
import com.example.project2.repository.PropertyRepository;
import com.example.project2.repository.DealRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/api/chat")
public class ChatApiController {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final AppUserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final DealRepository dealRepository;
    private final JdbcTemplate jdbcTemplate;

    public ChatApiController(ChatRepository chatRepository, MessageRepository messageRepository, AppUserRepository userRepository, PropertyRepository propertyRepository, DealRepository dealRepository, JdbcTemplate jdbcTemplate) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.dealRepository = dealRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/{chatId}/send-contract")
    public ResponseEntity<Map<String, Object>> sendContract(@PathVariable Long chatId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.status(401).build();

        Optional<Chat> chatOpt = chatRepository.findById(chatId);
        if (chatOpt.isEmpty()) return ResponseEntity.notFound().build();

        Chat chat = chatOpt.get();
        AppUser current = userOpt.get();

        // Только продавец может отправлять договор
        if (chat.getSeller() == null || !chat.getSeller().getId().equals(current.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Отправить договор может только продавец"));
        }

        // Проверка статуса объекта
        if (chat.getProperty() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Объявление не найдено для чата"));
        }
        if (chat.getProperty().getStatus() == PropertyStatus.sold) {
            return ResponseEntity.badRequest().body(Map.of("error", "Объект уже продан"));
        }

        // Если в чате уже есть сделка, повторно не создаем
        if (chat.getDeal() != null) {
            Long dealId = chat.getDeal().getId();
            // Отправим повторно ссылку
            Message msg = new Message();
            msg.setChat(chat);
            msg.setUser(current);
            msg.setMessageText("Договор отправлен: /deal/" + dealId);
            messageRepository.save(msg);
            return ResponseEntity.ok(Map.of("dealId", dealId));
        }

        // Создаем сделку
        Deal deal = new Deal();
        deal.setProperty(chat.getProperty());
        deal.setBuyer(chat.getBuyer());
        deal.setSeller(chat.getSeller());
        // Риелтора возьмем из property, если указан
        if (chat.getProperty().getRealtor() != null) {
            deal.setRealtor(chat.getProperty().getRealtor());
        }
        deal.setStatus(DealStatus.pending);
        deal = dealRepository.save(deal);

        // Привяжем сделку к чату
        chat.setDeal(deal);
        chatRepository.save(chat);

        // Отправим ссылку сообщением
        Message message = new Message();
        message.setChat(chat);
        message.setUser(current);
        message.setMessageText("Договор отправлен: /deal/" + deal.getId());
        messageRepository.save(message);

        return ResponseEntity.ok(Map.of("dealId", deal.getId()));
    }

    @PostMapping("/{chatId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(@PathVariable Long chatId, @RequestBody Map<String, String> request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.<MessageResponse>status(401).build();
        }
        
        Optional<Chat> chatOpt = chatRepository.findById(chatId);
        if (chatOpt.isEmpty()) {
            return ResponseEntity.<MessageResponse>notFound().build();
        }
        
        Chat chat = chatOpt.get();
        AppUser user = userOpt.get();
        
        // Инициализируем lazy loading для buyer и seller перед проверкой
        Long buyerId = null;
        Long sellerId = null;
        if (chat.getBuyer() != null) {
            buyerId = chat.getBuyer().getId();
        }
        if (chat.getSeller() != null) {
            sellerId = chat.getSeller().getId();
        }
        
        // Проверяем, что пользователь является участником чата
        if (buyerId == null || sellerId == null || (!buyerId.equals(user.getId()) && !sellerId.equals(user.getId()))) {
            return ResponseEntity.<MessageResponse>status(403).build();
        }
        
        String messageText = request.get("messageText");
        if (messageText == null || messageText.isBlank()) {
            return ResponseEntity.<MessageResponse>badRequest().build();
        }
        
        Message message = new Message();
        message.setChat(chat);
        message.setUser(user);
        message.setMessageText(messageText);
        
        Message saved = messageRepository.save(message);
        
        // Инициализируем lazy loading для user в сохраненном сообщении
        String userEmail = null;
        String userFirstName = null;
        String userLastName = null;
        Long userId = null;
        if (saved.getUser() != null) {
            userId = saved.getUser().getId();
            userEmail = saved.getUser().getEmail();
            userFirstName = saved.getUser().getFirstName();
            userLastName = saved.getUser().getLastName();
        }
        
        // Создаем DTO для ответа
        MessageResponse response = new MessageResponse(
                saved.getId(),
                userId,
                userEmail,
                userFirstName,
                userLastName,
                saved.getMessageText(),
                saved.getSentDate()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(@PathVariable Long chatId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        
        Optional<Chat> chatOpt = chatRepository.findById(chatId);
        if (chatOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Chat chat = chatOpt.get();
        AppUser user = userOpt.get();
        
        // Инициализируем lazy loading для buyer и seller перед проверкой
        Long buyerId = null;
        Long sellerId = null;
        if (chat.getBuyer() != null) {
            buyerId = chat.getBuyer().getId();
        }
        if (chat.getSeller() != null) {
            sellerId = chat.getSeller().getId();
        }
        
        // Проверяем, что пользователь является участником чата
        if (buyerId == null || sellerId == null || (!buyerId.equals(user.getId()) && !sellerId.equals(user.getId()))) {
            return ResponseEntity.status(403).build();
        }
        
        // Отмечаем чат как прочитанный
        markChatAsRead(chatId, user.getId());
        
        // Определяем собеседника для проверки прочитанных сообщений
        Long otherUserId = null;
        if (buyerId != null && buyerId.equals(user.getId())) {
            otherUserId = sellerId;
        } else if (sellerId != null && sellerId.equals(user.getId())) {
            otherUserId = buyerId;
        }
        
        // Получаем время последнего прочтения чата собеседником (для своих сообщений)
        LocalDateTime otherUserLastReadDate = otherUserId != null ? getLastReadDate(chatId, otherUserId) : null;
        
        List<Message> messages = messageRepository.findByChatIdOrderBySentDate(chatId);
        
        // Преобразуем в DTO для ответа
        List<MessageResponse> messageResponses = messages.stream().map(msg -> {
            Long userId = null;
            String userEmail = null;
            String userFirstName = null;
            String userLastName = null;
            
            if (msg.getUser() != null) {
                userId = msg.getUser().getId();
                userEmail = msg.getUser().getEmail();
                userFirstName = msg.getUser().getFirstName();
                userLastName = msg.getUser().getLastName();
            }
            
            // Определяем, прочитано ли сообщение
            boolean isRead = false;
            if (msg.getUser() != null && msg.getUser().getId().equals(user.getId())) {
                // Свое сообщение - проверяем, прочитал ли его собеседник
                isRead = otherUserLastReadDate != null && msg.getSentDate() != null && 
                        !msg.getSentDate().isAfter(otherUserLastReadDate);
            } else {
                // Сообщение от другого пользователя - считаем прочитанным, так как мы открыли чат
                // (время последнего прочтения уже обновлено выше)
                isRead = true;
            }
            
            MessageResponse response = new MessageResponse(
                    msg.getId(),
                    userId,
                    userEmail,
                    userFirstName,
                    userLastName,
                    msg.getMessageText(),
                    msg.getSentDate()
            );
            response.setRead(isRead);
            return response;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(messageResponses);
    }
    
    @PostMapping("/{chatId}/mark-read")
    @Transactional
    public ResponseEntity<Void> markChatAsRead(@PathVariable Long chatId) {
        System.out.println("=== API mark-read вызван для chatId=" + chatId);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            System.out.println("✗ Пользователь не найден");
            return ResponseEntity.status(401).build();
        }
        
        Optional<Chat> chatOpt = chatRepository.findById(chatId);
        if (chatOpt.isEmpty()) {
            System.out.println("✗ Чат не найден");
            return ResponseEntity.notFound().build();
        }
        
        Chat chat = chatOpt.get();
        AppUser user = userOpt.get();
        
        // Проверяем, что пользователь является участником чата
        Long buyerId = null;
        Long sellerId = null;
        if (chat.getBuyer() != null) {
            buyerId = chat.getBuyer().getId();
        }
        if (chat.getSeller() != null) {
            sellerId = chat.getSeller().getId();
        }
        
        if (buyerId == null || sellerId == null || (!buyerId.equals(user.getId()) && !sellerId.equals(user.getId()))) {
            System.out.println("✗ Пользователь не является участником чата");
            return ResponseEntity.status(403).build();
        }
        
        System.out.println("✓ Вызываем markChatAsRead для chatId=" + chatId + ", userId=" + user.getId());
        markChatAsRead(chatId, user.getId());
        
        // Проверяем сразу после вызова
        LocalDateTime savedDate = getLastReadDate(chatId, user.getId());
        System.out.println("=== После markChatAsRead: lastReadDate=" + savedDate);
        
        return ResponseEntity.ok().build();
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
                    System.out.println("✓ Чат " + chatId + " отмечен как прочитанный для пользователя " + userId + " (создана новая запись)");
                } catch (Exception insertEx) {
                    // Если INSERT не удался (например, из-за уникального ограничения), пробуем UPDATE еще раз
                    updated = jdbcTemplate.update(
                        "UPDATE chat_reads SET last_read_date = ? WHERE chat_id = ? AND user_id = ?",
                        now, chatId, userId
                    );
                    System.out.println("✓ Чат " + chatId + " отмечен как прочитанный для пользователя " + userId + " (обновлена после INSERT конфликта, строк: " + updated + ")");
                }
            } else {
                System.out.println("✓ Чат " + chatId + " отмечен как прочитанный для пользователя " + userId + " (обновлена существующая запись)");
            }
            
            // Проверяем, что запись действительно сохранена
            LocalDateTime savedDate = getLastReadDate(chatId, userId);
            if (savedDate == null) {
                System.err.println("⚠ ВНИМАНИЕ: Запись не найдена сразу после сохранения! chatId=" + chatId + ", userId=" + userId);
            } else {
                System.out.println("✓ Подтверждено: last_read_date сохранен как " + savedDate);
            }
        } catch (org.springframework.jdbc.BadSqlGrammarException e) {
            System.err.println("✗ SQL ошибка при отметке чата как прочитанного (таблица не существует): " + e.getMessage());
            // Пытаемся создать таблицу
            try {
                ensureChatReadsTableExists();
                // Повторяем попытку после создания таблицы
                markChatAsRead(chatId, userId);
            } catch (Exception retryEx) {
                System.err.println("✗ Не удалось создать таблицу или повторить операцию: " + retryEx.getMessage());
            }
        } catch (Exception e) {
            System.err.println("✗ Ошибка при отметке чата как прочитанного: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void ensureChatReadsTableExists() {
        try {
            // Проверяем, существует ли таблица
            jdbcTemplate.queryForObject("SELECT 1 FROM chat_reads LIMIT 1", Integer.class);
            System.out.println("DEBUG [ensureChatReadsTableExists]: Таблица chat_reads существует");
        } catch (Exception e) {
            // Таблица не существует, создаем её
            System.out.println("⚠ [ChatApiController] Таблица chat_reads не существует, создаем...");
            System.out.println("DEBUG [ensureChatReadsTableExists]: Ошибка при проверке таблицы: " + e.getMessage());
            try {
                // Сначала создаем таблицу без внешних ключей, чтобы избежать проблем
                jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS chat_reads (
                        id BIGSERIAL PRIMARY KEY,
                        chat_id BIGINT NOT NULL,
                        user_id BIGINT NOT NULL,
                        last_read_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(chat_id, user_id)
                    )
                    """);
                System.out.println("✓ [ChatApiController] Таблица chat_reads создана (без внешних ключей)");
                
                // Пытаемся добавить внешние ключи отдельно
                try {
                    jdbcTemplate.execute("""
                        ALTER TABLE chat_reads 
                        ADD CONSTRAINT fk_chat_reads_chat 
                        FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE
                        """);
                    System.out.println("✓ [ChatApiController] Внешний ключ для chat_id добавлен");
                } catch (Exception fk1Ex) {
                    System.out.println("⚠ [ChatApiController] Не удалось добавить внешний ключ для chat_id (возможно, уже существует): " + fk1Ex.getMessage());
                }
                
                try {
                    jdbcTemplate.execute("""
                        ALTER TABLE chat_reads 
                        ADD CONSTRAINT fk_chat_reads_user 
                        FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
                        """);
                    System.out.println("✓ [ChatApiController] Внешний ключ для user_id добавлен");
                } catch (Exception fk2Ex) {
                    System.out.println("⚠ [ChatApiController] Не удалось добавить внешний ключ для user_id (возможно, уже существует): " + fk2Ex.getMessage());
                }
                
                // Создаем уникальный индекс
                try {
                    jdbcTemplate.execute("""
                        CREATE UNIQUE INDEX IF NOT EXISTS chat_reads_chat_user_unique 
                        ON chat_reads(chat_id, user_id)
                        """);
                    System.out.println("✓ [ChatApiController] Уникальный индекс создан");
                } catch (Exception indexEx) {
                    System.out.println("⚠ [ChatApiController] Не удалось создать уникальный индекс (возможно, уже существует): " + indexEx.getMessage());
                }
                
                System.out.println("✓ [ChatApiController] Таблица chat_reads полностью создана");
            } catch (Exception createEx) {
                System.err.println("✗ [ChatApiController] Не удалось создать таблицу chat_reads: " + createEx.getMessage());
                createEx.printStackTrace();
                throw createEx;
            }
        }
    }
    
    private LocalDateTime getLastReadDate(Long chatId, Long userId) {
        try {
            // Сначала убеждаемся, что таблица существует
            ensureChatReadsTableExists();
            
            // Используем более надежный запрос с обработкой пустого результата
            List<LocalDateTime> results = jdbcTemplate.query(
                "SELECT last_read_date FROM chat_reads WHERE chat_id = ? AND user_id = ?",
                (rs, rowNum) -> {
                    java.sql.Timestamp timestamp = rs.getTimestamp("last_read_date");
                    return timestamp != null ? timestamp.toLocalDateTime() : null;
                },
                chatId, userId
            );
            if (results.isEmpty()) {
                System.out.println("DEBUG [getLastReadDate]: Чат " + chatId + ", пользователь " + userId + ": null (запись не найдена в БД)");
                return null;
            }
            LocalDateTime result = results.get(0);
            System.out.println("DEBUG [getLastReadDate]: Чат " + chatId + ", пользователь " + userId + ": " + result);
            return result;
        } catch (org.springframework.jdbc.BadSqlGrammarException e) {
            System.out.println("DEBUG [getLastReadDate]: SQL ошибка - таблица или колонка не существует: " + e.getMessage());
            // Пытаемся создать таблицу, если её нет
            try {
                ensureChatReadsTableExists();
                // Повторяем попытку после создания таблицы
                return getLastReadDate(chatId, userId);
            } catch (Exception retryEx) {
                System.out.println("DEBUG [getLastReadDate]: Не удалось создать таблицу или повторить запрос: " + retryEx.getMessage());
                return null;
            }
        } catch (Exception e) {
            System.out.println("DEBUG [getLastReadDate]: Чат " + chatId + ", пользователь " + userId + ": null (ошибка: " + e.getMessage() + ")");
            e.printStackTrace();
            return null;
        }
    }
    
    @PostMapping("/create")
    @Transactional
    public ResponseEntity<Map<String, Object>> createChat(@RequestBody Map<String, Object> request) {
        Long propertyId;
        try {
            Object propIdObj = request.get("propertyId");
            if (propIdObj == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "propertyId is required"));
            }
            if (propIdObj instanceof Number) {
                propertyId = ((Number) propIdObj).longValue();
            } else if (propIdObj instanceof String) {
                propertyId = Long.parseLong((String) propIdObj);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid propertyId format"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid propertyId: " + e.getMessage()));
        }
        try {
            System.out.println("DEBUG [createChat]: Запрос на создание чата для propertyId=" + propertyId);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            Optional<AppUser> currentUserOpt = userRepository.findByEmail(email);
            
            if (currentUserOpt.isEmpty()) {
                System.out.println("DEBUG [createChat]: Пользователь не найден");
                return ResponseEntity.status(401).build();
            }
            
            AppUser currentUser = currentUserOpt.get();
            System.out.println("DEBUG [createChat]: Текущий пользователь: " + currentUser.getId() + " (" + email + ")");
            
            // Загружаем property с user (используем метод с JOIN FETCH если доступен)
            Optional<com.example.project2.model.Property> propertyOpt = propertyRepository.findById(propertyId);
            if (propertyOpt.isEmpty()) {
                System.out.println("DEBUG [createChat]: Property не найдена");
                return ResponseEntity.notFound().build();
            }
            
            com.example.project2.model.Property property = propertyOpt.get();
            // Инициализируем lazy loading для user
            if (property.getUser() != null) {
                property.getUser().getEmail();
                System.out.println("DEBUG [createChat]: Автор property: " + property.getUser().getId() + " (" + property.getUser().getEmail() + ")");
            } else {
                System.out.println("DEBUG [createChat]: Property не имеет автора");
            }
            
            // Проверяем, является ли текущий пользователь автором объявления
            boolean isOwner = property.getUser() != null && property.getUser().getId().equals(currentUser.getId());
            if (isOwner) {
                System.out.println("DEBUG [createChat]: Пользователь пытается написать самому себе");
                return ResponseEntity.status(403).body(Map.of("error", "Вы не можете написать самому себе"));
            }
            
            AppUser buyer = currentUser;
            AppUser seller = property.getUser();
            
            // Если у property нет автора, пытаемся найти реальтора tamara@list.ru
            if (seller == null) {
                System.out.println("DEBUG [createChat]: Property не имеет автора, ищем реальтора tamara@list.ru");
                Optional<AppUser> tamaraOpt = userRepository.findByEmail("tamara@list.ru");
                if (tamaraOpt.isPresent()) {
                    seller = tamaraOpt.get();
                    // Назначаем автора property
                    property.setUser(seller);
                    propertyRepository.save(property);
                    System.out.println("DEBUG [createChat]: Автор property назначен: " + seller.getId() + " (" + seller.getEmail() + ")");
                } else {
                    System.out.println("DEBUG [createChat]: Автор объявления не найден и tamara@list.ru не найден");
                    return ResponseEntity.status(400).body(Map.of("error", "Автор объявления не найден"));
                }
            }
            
            // Ищем существующий чат
            Chat existingChat = chatRepository.findByPropertyAndUsers(propertyId, buyer.getId(), seller.getId());
            if (existingChat != null) {
                System.out.println("DEBUG [createChat]: Найден существующий чат: " + existingChat.getId());
                return ResponseEntity.ok(Map.of("chatId", existingChat.getId(), "created", false));
            }
            
            // Создаем новый чат
            System.out.println("DEBUG [createChat]: Создание нового чата...");
            Chat chat = new Chat();
            chat.setProperty(property);
            chat.setBuyer(buyer);
            chat.setSeller(seller);
            chat = chatRepository.save(chat);
            System.out.println("DEBUG [createChat]: Чат создан с ID: " + chat.getId());
            
            return ResponseEntity.ok(Map.of("chatId", chat.getId(), "created", true));
        } catch (Exception e) {
            System.err.println("DEBUG [createChat]: Ошибка при создании чата: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Внутренняя ошибка сервера: " + e.getMessage()));
        }
    }
    
    @GetMapping("/my-chats")
    public ResponseEntity<List<com.example.project2.dto.ChatListItemResponse>> getMyChats() {
        System.out.println("=== getMyChats вызван");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        
        AppUser currentUser = userOpt.get();
        System.out.println("=== getMyChats для пользователя " + currentUser.getId() + " (" + email + ")");
        List<Chat> chats = chatRepository.findByUserInvolved(currentUser.getId());
        System.out.println("=== Найдено чатов: " + chats.size());
        
        // Преобразуем в DTO и фильтруем пустые чаты
        List<com.example.project2.dto.ChatListItemResponse> chatList = chats.stream().map(chat -> {
            // Инициализируем lazy loading
            AppUser otherUser = null;
            if (chat.getBuyer() != null && chat.getBuyer().getId().equals(currentUser.getId())) {
                otherUser = chat.getSeller();
            } else {
                otherUser = chat.getBuyer();
            }
            
            // Инициализируем property
            String propertyAddress = null;
            Long propertyId = null;
            if (chat.getProperty() != null) {
                propertyId = chat.getProperty().getId();
                propertyAddress = chat.getProperty().getAddress();
            }
            
            // Инициализируем otherUser
            String otherUserEmail = null;
            String otherUserFirstName = null;
            String otherUserLastName = null;
            Long otherUserId = null;
            if (otherUser != null) {
                otherUserId = otherUser.getId();
                otherUserEmail = otherUser.getEmail();
                otherUserFirstName = otherUser.getFirstName();
                otherUserLastName = otherUser.getLastName();
            }
            
            // Получаем последнее сообщение
            List<Message> messages = messageRepository.findByChatIdOrderBySentDate(chat.getId());
            String lastMessageText = null;
            java.time.LocalDateTime lastMessageDate = null;
            if (!messages.isEmpty()) {
                Message lastMessage = messages.get(messages.size() - 1);
                lastMessageText = lastMessage.getMessageText();
                lastMessageDate = lastMessage.getSentDate();
            }
            
            // Пропускаем пустые чаты (без сообщений)
            if (messages.isEmpty()) {
                return null;
            }
            
            com.example.project2.dto.ChatListItemResponse response = new com.example.project2.dto.ChatListItemResponse();
            response.setId(chat.getId());
            response.setPropertyId(propertyId);
            response.setPropertyAddress(propertyAddress);
            response.setOtherUserId(otherUserId);
            response.setOtherUserEmail(otherUserEmail);
            response.setOtherUserFirstName(otherUserFirstName);
            response.setOtherUserLastName(otherUserLastName);
            response.setLastMessageText(lastMessageText);
            response.setLastMessageDate(lastMessageDate);
            response.setCreatedDate(chat.getCreatedDate());
            
            // Подсчитываем непрочитанные сообщения с учетом времени последнего прочтения
            Long unreadCount = 0L;
            if (!messages.isEmpty()) {
                Message lastMessage = messages.get(messages.size() - 1);
                // Если последнее сообщение от другого пользователя, считаем непрочитанные
                if (lastMessage.getUser() != null && !lastMessage.getUser().getId().equals(currentUser.getId())) {
                    // Получаем lastReadDate - делаем несколько попыток для надежности
                    LocalDateTime lastReadDate = getLastReadDate(chat.getId(), currentUser.getId());
                    
                    // Если не найдено, пробуем еще раз с небольшой задержкой (возможно, запись еще не закоммичена)
                    if (lastReadDate == null) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        lastReadDate = getLastReadDate(chat.getId(), currentUser.getId());
                    }
                    
                    if (lastReadDate != null) {
                        // Считаем сообщения, отправленные после последнего прочтения
                        unreadCount = messageRepository.countUnreadMessagesAfterDate(
                            chat.getId(), currentUser.getId(), lastReadDate
                        );
                        System.out.println("DEBUG [getMyChats]: Чат " + chat.getId() + ", пользователь " + currentUser.getId() + 
                            ", lastReadDate: " + lastReadDate + ", непрочитанных: " + unreadCount);
                    } else {
                        // Если чат никогда не был прочитан, считаем все сообщения от другого пользователя
                        unreadCount = messageRepository.countUnreadMessages(chat.getId(), currentUser.getId());
                        System.out.println("DEBUG [getMyChats]: Чат " + chat.getId() + ", пользователь " + currentUser.getId() + 
                            ", lastReadDate: null, непрочитанных (все): " + unreadCount);
                    }
                } else {
                    System.out.println("DEBUG: Чат " + chat.getId() + ", последнее сообщение от текущего пользователя, непрочитанных: 0");
                }
            }
            response.setUnreadCount(unreadCount);
            
            return response;
        })
        .filter(chat -> chat != null) // Фильтруем null (пустые чаты) ПЕРЕД сортировкой
        .sorted((c1, c2) -> {
            // Сортируем по дате последнего сообщения (новые первыми)
            // Если нет последнего сообщения, используем дату создания чата
            java.time.LocalDateTime date1 = c1.getLastMessageDate() != null ? c1.getLastMessageDate() : c1.getCreatedDate();
            java.time.LocalDateTime date2 = c2.getLastMessageDate() != null ? c2.getLastMessageDate() : c2.getCreatedDate();
            
            if (date1 == null && date2 == null) {
                return 0;
            }
            if (date1 == null) {
                return 1;
            }
            if (date2 == null) {
                return -1;
            }
            return date2.compareTo(date1);
        })
        .collect(Collectors.toList());
        
        return ResponseEntity.ok(chatList);
    }
    
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        
        AppUser currentUser = userOpt.get();
        List<Chat> chats = chatRepository.findByUserInvolved(currentUser.getId());
        
        // Подсчитываем общее количество непрочитанных сообщений с учетом времени последнего прочтения
        Long totalUnread = 0L;
        for (Chat chat : chats) {
            List<Message> messages = messageRepository.findByChatIdOrderBySentDate(chat.getId());
            if (!messages.isEmpty()) {
                Message lastMessage = messages.get(messages.size() - 1);
                // Если последнее сообщение от другого пользователя, считаем непрочитанные
                if (lastMessage.getUser() != null && !lastMessage.getUser().getId().equals(currentUser.getId())) {
                    LocalDateTime lastReadDate = getLastReadDate(chat.getId(), currentUser.getId());
                    Long unread;
                    if (lastReadDate != null) {
                        unread = messageRepository.countUnreadMessagesAfterDate(
                            chat.getId(), currentUser.getId(), lastReadDate
                        );
                    } else {
                        unread = messageRepository.countUnreadMessages(chat.getId(), currentUser.getId());
                    }
                    totalUnread += unread;
                }
            }
        }
        
        return ResponseEntity.ok(Map.of("unreadCount", totalUnread));
    }
    
    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteChat(@PathVariable Long chatId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        
        Optional<Chat> chatOpt = chatRepository.findById(chatId);
        if (chatOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Chat chat = chatOpt.get();
        AppUser currentUser = userOpt.get();
        
        // Проверяем, что пользователь является участником чата
        Long buyerId = null;
        Long sellerId = null;
        if (chat.getBuyer() != null) {
            buyerId = chat.getBuyer().getId();
        }
        if (chat.getSeller() != null) {
            sellerId = chat.getSeller().getId();
        }
        
        if (buyerId == null || sellerId == null || (!buyerId.equals(currentUser.getId()) && !sellerId.equals(currentUser.getId()))) {
            return ResponseEntity.status(403).build();
        }
        
        // Проверяем, что чат пустой (нет сообщений)
        List<Message> messages = messageRepository.findByChatIdOrderBySentDate(chatId);
        if (messages.isEmpty()) {
            chatRepository.deleteById(chatId);
            return ResponseEntity.noContent().build();
        }
        
        return ResponseEntity.badRequest().build();
    }
}

