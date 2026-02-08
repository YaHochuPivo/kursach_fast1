package com.example.project2.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "property_registry")
public class PropertyRegistry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Адрес недвижимости обязателен")
    @Size(max = 255, message = "Адрес слишком длинный")
    @Column(name = "property_address")
    private String propertyAddress;

    @Column(name = "debt_exists")
    private Boolean debtExists = false;

    @Column(name = "debt_amount")
    private Float debtAmount;

    @Column(columnDefinition = "TEXT")
    private String problems;

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPropertyAddress() { return propertyAddress; }
    public void setPropertyAddress(String propertyAddress) { this.propertyAddress = propertyAddress; }

    public Boolean getDebtExists() { return debtExists; }
    public void setDebtExists(Boolean debtExists) { this.debtExists = debtExists; }

    public Float getDebtAmount() { return debtAmount; }
    public void setDebtAmount(Float debtAmount) { this.debtAmount = debtAmount; }

    public String getProblems() { return problems; }
    public void setProblems(String problems) { this.problems = problems; }
}
