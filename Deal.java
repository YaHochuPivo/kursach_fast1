package com.example.project2.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

@Entity
@Table(name = "deals")
public class Deal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @NotNull(message = "Недвижимость обязательна")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    @NotNull(message = "Покупатель обязателен")
    private AppUser buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    @NotNull(message = "Продавец обязателен")
    private AppUser seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "realtor_id")
    private AppUser realtor;

    @Column(name = "deal_date")
    private LocalDateTime dealDate;

    @Enumerated(EnumType.STRING)
    private DealStatus status;

    @Column(name = "final_price")
    @Positive(message = "Финальная цена должна быть положительной")
    private Float finalPrice;

    // Конструкторы
    public Deal() {
        this.dealDate = LocalDateTime.now();
        this.status = DealStatus.pending;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }

    public AppUser getBuyer() { return buyer; }
    public void setBuyer(AppUser buyer) { this.buyer = buyer; }

    public AppUser getSeller() { return seller; }
    public void setSeller(AppUser seller) { this.seller = seller; }

    public AppUser getRealtor() { return realtor; }
    public void setRealtor(AppUser realtor) { this.realtor = realtor; }

    public LocalDateTime getDealDate() { return dealDate; }
    public void setDealDate(LocalDateTime dealDate) { this.dealDate = dealDate; }

    public DealStatus getStatus() { return status; }
    public void setStatus(DealStatus status) { this.status = status; }

    public Float getFinalPrice() { return finalPrice; }
    public void setFinalPrice(Float finalPrice) { this.finalPrice = finalPrice; }
}
