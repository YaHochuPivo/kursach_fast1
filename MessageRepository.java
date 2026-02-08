package com.example.project2.repository;

import com.example.project2.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    @Query("SELECT m FROM Message m LEFT JOIN FETCH m.user WHERE m.chat.id = :chatId ORDER BY m.sentDate ASC")
    List<Message> findByChatIdOrderBySentDate(@Param("chatId") Long chatId);
    
    @Query("SELECT m FROM Message m WHERE m.user.id = :userId ORDER BY m.sentDate DESC")
    List<Message> findByUserIdOrderBySentDate(@Param("userId") Long userId);
    
    // Подсчет непрочитанных сообщений в чате (сообщения, отправленные не текущим пользователем)
    @Query("SELECT COUNT(m) FROM Message m WHERE m.chat.id = :chatId AND m.user.id != :userId")
    Long countUnreadMessages(@Param("chatId") Long chatId, @Param("userId") Long userId);
    
    // Подсчет непрочитанных сообщений в чате после определенной даты
    @Query("SELECT COUNT(m) FROM Message m WHERE m.chat.id = :chatId AND m.user.id != :userId AND m.sentDate > :lastReadDate")
    Long countUnreadMessagesAfterDate(@Param("chatId") Long chatId, @Param("userId") Long userId, @Param("lastReadDate") java.time.LocalDateTime lastReadDate);
}
