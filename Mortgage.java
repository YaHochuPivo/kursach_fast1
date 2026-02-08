package com.example.project2.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Mortgage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id", unique = true, nullable = false)
    private Deal deal;

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private BigDecimal amount; // сумма кредита (после первоначального взноса)

    @Column(nullable = false)
    private Integer termMonths;

    @Column(nullable = false)
    private BigDecimal rate; // годовая ставка, %

    @Column
    private BigDecimal downPayment; // первоначальный взнос

    @Column
    private BigDecimal monthlyPayment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MortgageStatus status = MortgageStatus.REQUESTED;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // getters/setters
    public Long getId() { return id; }
    public Deal getDeal() { return deal; }
    public void setDeal(Deal deal) { this.deal = deal; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getTermMonths() { return termMonths; }
    public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public BigDecimal getDownPayment() { return downPayment; }
    public void setDownPayment(BigDecimal downPayment) { this.downPayment = downPayment; }
    public BigDecimal getMonthlyPayment() { return monthlyPayment; }
    public void setMonthlyPayment(BigDecimal monthlyPayment) { this.monthlyPayment = monthlyPayment; }
    public MortgageStatus getStatus() { return status; }
    public void setStatus(MortgageStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
