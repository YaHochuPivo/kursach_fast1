package com.example.project2.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class BankApproval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id", unique = true)
    private Deal deal;

    @Column(nullable = false)
    private String bankName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BankApprovalStatus status = BankApprovalStatus.REQUESTED;

    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Deal getDeal() { return deal; }
    public void setDeal(Deal deal) { this.deal = deal; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public BankApprovalStatus getStatus() { return status; }
    public void setStatus(BankApprovalStatus status) { this.status = status; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
