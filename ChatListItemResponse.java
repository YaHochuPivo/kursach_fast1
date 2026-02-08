package com.example.project2.dto;

import java.time.LocalDateTime;

public class ChatListItemResponse {
    private Long id;
    private Long propertyId;
    private String propertyAddress;
    private Long otherUserId;
    private String otherUserEmail;
    private String otherUserFirstName;
    private String otherUserLastName;
    private String lastMessageText;
    private LocalDateTime lastMessageDate;
    private LocalDateTime createdDate;
    private Long unreadCount;

    public ChatListItemResponse() {
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPropertyId() { return propertyId; }
    public void setPropertyId(Long propertyId) { this.propertyId = propertyId; }

    public String getPropertyAddress() { return propertyAddress; }
    public void setPropertyAddress(String propertyAddress) { this.propertyAddress = propertyAddress; }

    public Long getOtherUserId() { return otherUserId; }
    public void setOtherUserId(Long otherUserId) { this.otherUserId = otherUserId; }

    public String getOtherUserEmail() { return otherUserEmail; }
    public void setOtherUserEmail(String otherUserEmail) { this.otherUserEmail = otherUserEmail; }

    public String getOtherUserFirstName() { return otherUserFirstName; }
    public void setOtherUserFirstName(String otherUserFirstName) { this.otherUserFirstName = otherUserFirstName; }

    public String getOtherUserLastName() { return otherUserLastName; }
    public void setOtherUserLastName(String otherUserLastName) { this.otherUserLastName = otherUserLastName; }

    public String getLastMessageText() { return lastMessageText; }
    public void setLastMessageText(String lastMessageText) { this.lastMessageText = lastMessageText; }

    public LocalDateTime getLastMessageDate() { return lastMessageDate; }
    public void setLastMessageDate(LocalDateTime lastMessageDate) { this.lastMessageDate = lastMessageDate; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public Long getUnreadCount() { return unreadCount; }
    public void setUnreadCount(Long unreadCount) { this.unreadCount = unreadCount; }
}

