package com.example.project2.dto;

import java.time.LocalDateTime;

public class MessageResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private String userFirstName;
    private String userLastName;
    private String messageText;
    private LocalDateTime sentDate;
    private Boolean read;

    public MessageResponse() {
    }

    public MessageResponse(Long id, Long userId, String userEmail, String userFirstName, String userLastName, String messageText, LocalDateTime sentDate) {
        this.id = id;
        this.userId = userId;
        this.userEmail = userEmail;
        this.userFirstName = userFirstName;
        this.userLastName = userLastName;
        this.messageText = messageText;
        this.sentDate = sentDate;
        this.read = false;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserFirstName() { return userFirstName; }
    public void setUserFirstName(String userFirstName) { this.userFirstName = userFirstName; }

    public String getUserLastName() { return userLastName; }
    public void setUserLastName(String userLastName) { this.userLastName = userLastName; }

    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }

    public LocalDateTime getSentDate() { return sentDate; }
    public void setSentDate(LocalDateTime sentDate) { this.sentDate = sentDate; }
    
    public Boolean getRead() { return read; }
    public void setRead(Boolean read) { this.read = read; }
}

