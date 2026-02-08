package com.example.project2.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String passwordHash;

    private String firstName;

    private String lastName;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "registration_date")
    private LocalDateTime registrationDate;

    private String realtorLicense;

    // Необязательные паспортные данные для обычных пользователей
    private String passportData;

    // Серия паспорта (4 цифры), необязательное поле
    private String passportSeries;

    // Номер паспорта (6 цифр), необязательное поле
    private String passportNumber;
    private java.math.BigDecimal serviceFee;

    // Конструкторы
    public AppUser() {
        this.registrationDate = LocalDateTime.now();
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }

    public String getRealtorLicense() { return realtorLicense; }
    public void setRealtorLicense(String realtorLicense) { this.realtorLicense = realtorLicense; }

    public String getPassportData() { return passportData; }
    public void setPassportData(String passportData) { this.passportData = passportData; }

    public String getPassportSeries() { return passportSeries; }
    public void setPassportSeries(String passportSeries) { this.passportSeries = passportSeries; }

    public String getPassportNumber() { return passportNumber; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }

    public java.math.BigDecimal getServiceFee() { return serviceFee; }
    public void setServiceFee(java.math.BigDecimal serviceFee) { this.serviceFee = serviceFee; }

    // Удобные методы
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isRealtor() {
        return role == UserRole.REALTOR;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}


