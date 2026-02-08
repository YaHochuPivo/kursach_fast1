package com.example.project2.repository;

import com.example.project2.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    
    @Query("SELECT c FROM Contract c WHERE c.deal.id = :dealId")
    Contract findByDealId(@Param("dealId") Long dealId);
    
    @Query("SELECT c FROM Contract c WHERE c.signedBuyerDate IS NOT NULL AND c.signedSellerDate IS NOT NULL")
    List<Contract> findFullySignedContracts();
    
    @Query("SELECT c FROM Contract c WHERE c.signedBuyerDate IS NULL OR c.signedSellerDate IS NULL")
    List<Contract> findPendingContracts();
}
