package com.example.project2.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_settings")
public class UserSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(name = "theme")
    private String theme; // light|dark

    @Column(name = "locale")
    private String locale; // ru-RU, en-US

    @Column(name = "number_format")
    private String numberFormat; // grouping, decimals

    @Column(name = "page_size")
    private Integer pageSize; // default page size

    @Column(name = "saved_filters", columnDefinition = "text")
    private String savedFilters; // JSON string with saved filters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public String getNumberFormat() { return numberFormat; }
    public void setNumberFormat(String numberFormat) { this.numberFormat = numberFormat; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public String getSavedFilters() { return savedFilters; }
    public void setSavedFilters(String savedFilters) { this.savedFilters = savedFilters; }
}
