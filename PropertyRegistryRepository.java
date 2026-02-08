package com.example.project2.repository;

import com.example.project2.model.PropertyRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyRegistryRepository extends JpaRepository<PropertyRegistry, Long> {
    
    @Query("SELECT pr FROM PropertyRegistry pr WHERE pr.propertyAddress LIKE %:address%")
    List<PropertyRegistry> findByPropertyAddressContaining(@Param("address") String address);
    
    @Query("SELECT pr FROM PropertyRegistry pr WHERE pr.debtExists = :debtExists")
    List<PropertyRegistry> findByDebtExists(@Param("debtExists") Boolean debtExists);
    
    @Query("SELECT pr FROM PropertyRegistry pr WHERE pr.debtAmount > :minDebt")
    List<PropertyRegistry> findByDebtAmountGreaterThan(@Param("minDebt") Float minDebt);
}
