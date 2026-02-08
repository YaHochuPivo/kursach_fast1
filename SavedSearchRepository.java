package com.example.project2.repository;

import com.example.project2.model.SavedSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavedSearchRepository extends JpaRepository<SavedSearch, Long> {
    
    @Query("SELECT ss FROM SavedSearch ss WHERE ss.user.id = :userId ORDER BY ss.lastUsedAt DESC NULLS LAST, ss.createdAt DESC")
    List<SavedSearch> findByUserIdOrderByLastUsedAtDesc(@Param("userId") Long userId);
    
    void deleteByUserId(Long userId);
}

