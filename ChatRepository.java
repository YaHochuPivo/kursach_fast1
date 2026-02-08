package com.example.project2.repository;

import com.example.project2.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    
    @Query("SELECT c FROM Chat c WHERE c.deal.id = :dealId")
    Chat findByDealId(@Param("dealId") Long dealId);
    
    @Query("SELECT c FROM Chat c WHERE c.property.id = :propertyId AND c.buyer.id = :buyerId AND c.seller.id = :sellerId")
    Chat findByPropertyAndUsers(@Param("propertyId") Long propertyId, @Param("buyerId") Long buyerId, @Param("sellerId") Long sellerId);
    
    @Query("SELECT DISTINCT c FROM Chat c LEFT JOIN FETCH c.buyer LEFT JOIN FETCH c.seller LEFT JOIN FETCH c.property WHERE c.property.id = :propertyId")
    List<Chat> findByPropertyId(@Param("propertyId") Long propertyId);
    
    @Query("SELECT DISTINCT c FROM Chat c LEFT JOIN FETCH c.buyer LEFT JOIN FETCH c.seller LEFT JOIN FETCH c.property WHERE c.buyer.id = :userId OR c.seller.id = :userId")
    List<Chat> findByUserInvolved(@Param("userId") Long userId);
    
    @Query("SELECT c FROM Chat c WHERE c.deal.buyer.id = :userId OR c.deal.seller.id = :userId OR c.deal.realtor.id = :userId")
    List<Chat> findByUserInvolvedInDeal(@Param("userId") Long userId);
}
