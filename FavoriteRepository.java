package com.example.project2.repository;

import com.example.project2.model.AppUser;
import com.example.project2.model.Favorite;
import com.example.project2.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    
    List<Favorite> findByUser(AppUser user);
    
    Optional<Favorite> findByUserAndProperty(AppUser user, Property property);
    
    @Query("SELECT f FROM Favorite f WHERE f.user.id = :userId")
    List<Favorite> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT f FROM Favorite f WHERE f.property.id = :propertyId")
    List<Favorite> findByPropertyId(@Param("propertyId") Long propertyId);
    
    void deleteByUserAndProperty(AppUser user, Property property);
}
