package com.example.project2.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @NotBlank(message = "Тип отчета обязателен")
    @Size(max = 100, message = "Тип отчета слишком длинный")
    @Column(name = "report_type")
    private String reportType;

    @Column(name = "generated_date")
    private LocalDateTime generatedDate;

    @Column(columnDefinition = "JSON")
    private String data;

    // Конструкторы
    public Report() {
        this.generatedDate = LocalDateTime.now();
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public LocalDateTime getGeneratedDate() { return generatedDate; }
    public void setGeneratedDate(LocalDateTime generatedDate) { this.generatedDate = generatedDate; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
}
