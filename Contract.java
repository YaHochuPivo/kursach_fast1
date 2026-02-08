package com.example.project2.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "contracts")
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id", nullable = false)
    private Deal deal;

    @NotBlank(message = "Текст договора обязателен")
    @Column(name = "contract_text", columnDefinition = "TEXT")
    private String contractText;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "signed_buyer_date")
    private LocalDateTime signedBuyerDate;

    @Column(name = "signed_seller_date")
    private LocalDateTime signedSellerDate;

    @Size(max = 255, message = "Путь к PDF слишком длинный")
    @Column(name = "pdf_path")
    private String pdfPath;

    // Конструкторы
    public Contract() {
        this.createdDate = LocalDateTime.now();
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Deal getDeal() { return deal; }
    public void setDeal(Deal deal) { this.deal = deal; }

    public String getContractText() { return contractText; }
    public void setContractText(String contractText) { this.contractText = contractText; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getSignedBuyerDate() { return signedBuyerDate; }
    public void setSignedBuyerDate(LocalDateTime signedBuyerDate) { this.signedBuyerDate = signedBuyerDate; }

    public LocalDateTime getSignedSellerDate() { return signedSellerDate; }
    public void setSignedSellerDate(LocalDateTime signedSellerDate) { this.signedSellerDate = signedSellerDate; }

    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }

    // Удобные методы
    public boolean isFullySigned() {
        return signedBuyerDate != null && signedSellerDate != null;
    }
}
