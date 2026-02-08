package com.example.project2.repository;

import com.example.project2.model.BankApproval;
import com.example.project2.model.Deal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankApprovalRepository extends JpaRepository<BankApproval, Long> {
    Optional<BankApproval> findByDeal(Deal deal);
}
