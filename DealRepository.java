package com.example.project2.repository;

import com.example.project2.model.Deal;
import com.example.project2.model.DealStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DealRepository extends JpaRepository<Deal, Long> {
    
    List<Deal> findByStatus(DealStatus status);
    
    @Query("SELECT d FROM Deal d WHERE d.buyer.id = :userId")
    List<Deal> findByBuyerId(@Param("userId") Long userId);
    
    @Query("SELECT d FROM Deal d WHERE d.seller.id = :userId")
    List<Deal> findBySellerId(@Param("userId") Long userId);
    
    @Query("SELECT d FROM Deal d WHERE d.realtor.id = :realtorId")
    List<Deal> findByRealtorId(@Param("realtorId") Long realtorId);
    
    @Query("SELECT d FROM Deal d WHERE d.property.id = :propertyId")
    List<Deal> findByPropertyId(@Param("propertyId") Long propertyId);
    
    @Query("SELECT d FROM Deal d WHERE (d.buyer.id = :userId OR d.seller.id = :userId OR d.realtor.id = :userId)")
    List<Deal> findByUserInvolved(@Param("userId") Long userId);

    @Query("SELECT d FROM Deal d WHERE ( (d.buyer IS NOT NULL AND d.buyer.email = :email) OR (d.seller IS NOT NULL AND d.seller.email = :email) OR (d.realtor IS NOT NULL AND d.realtor.email = :email) )")
    List<Deal> findByUserEmailInvolved(@Param("email") String email);

    // Case-insensitive email match variant to avoid missing deals due to email case differences
    @Query("SELECT d FROM Deal d WHERE ((d.buyer IS NOT NULL AND LOWER(d.buyer.email) = LOWER(:email)) OR (d.seller IS NOT NULL AND LOWER(d.seller.email) = LOWER(:email)) OR (d.realtor IS NOT NULL AND LOWER(d.realtor.email) = LOWER(:email)))")
    List<Deal> findByUserEmailInvolvedIgnoreCase(@Param("email") String email);
}
