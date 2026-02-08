package com.example.project2.repository;

import com.example.project2.model.Mortgage;
import com.example.project2.model.Deal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MortgageRepository extends JpaRepository<Mortgage, Long> {
    Optional<Mortgage> findByDeal(Deal deal);
}
