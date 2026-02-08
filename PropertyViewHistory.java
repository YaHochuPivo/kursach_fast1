package com.example.project2.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "property_view_history", indexes = {
    @Index(name = "idx_user_property", columnList = "user_id,property_id"),
    @Index(name = "idx_user_viewed_at", columnList = "user_id,viewed_at")
})
public class PropertyViewHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    @Column(name = "view_count")
    private Integer viewCount = 1;

    // Конструкторы
    public PropertyViewHistory() {
        this.viewedAt = LocalDateTime.now();
    }

    public PropertyViewHistory(AppUser user, Property property) {
        this();
        this.user = user;
        this.property = property;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }

    public LocalDateTime getViewedAt() { return viewedAt; }
    public void setViewedAt(LocalDateTime viewedAt) { this.viewedAt = viewedAt; }

    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
}

