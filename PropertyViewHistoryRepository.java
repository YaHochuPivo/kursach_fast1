package com.example.project2.repository;

import com.example.project2.model.PropertyViewHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyViewHistoryRepository extends JpaRepository<PropertyViewHistory, Long> {
    
    Optional<PropertyViewHistory> findByUserIdAndPropertyId(Long userId, Long propertyId);
    
    @Query("SELECT DISTINCT pvh FROM PropertyViewHistory pvh LEFT JOIN FETCH pvh.property LEFT JOIN FETCH pvh.property.user LEFT JOIN FETCH pvh.property.realtor WHERE pvh.user.id = :userId ORDER BY pvh.viewedAt DESC")
    List<PropertyViewHistory> findByUserIdOrderByViewedAtDesc(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT DISTINCT pvh FROM PropertyViewHistory pvh LEFT JOIN FETCH pvh.property LEFT JOIN FETCH pvh.property.user LEFT JOIN FETCH pvh.property.realtor WHERE pvh.user.id = :userId ORDER BY pvh.viewedAt DESC")
    List<PropertyViewHistory> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT pvh.property.id FROM PropertyViewHistory pvh WHERE pvh.user.id = :userId GROUP BY pvh.property.id ORDER BY MAX(pvh.viewedAt) DESC")
    List<Long> findDistinctPropertyIdsByUserId(@Param("userId") Long userId);
}

