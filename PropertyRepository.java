package com.example.project2.repository;

import com.example.project2.model.Property;
import com.example.project2.model.PropertyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {
    
    List<Property> findByStatus(PropertyStatus status);
    
    List<Property> findByType(String type);
    
    List<Property> findByCategory(String category);
    
    List<Property> findByPriceBetween(Float minPrice, Float maxPrice);
    
    List<Property> findByAreaBetween(Float minArea, Float maxArea);
    
    List<Property> findByRooms(Integer rooms);
    
    @Query("SELECT p FROM Property p WHERE p.address LIKE %:address%")
    List<Property> findByAddressContaining(@Param("address") String address);
    
    List<Property> findByAddress(String address);
    
    @Query("SELECT DISTINCT p FROM Property p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.realtor WHERE p.user.id = :userId")
    List<Property> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT p FROM Property p WHERE p.realtor.id = :realtorId")
    List<Property> findByRealtorId(@Param("realtorId") Long realtorId);
    
    @Query("SELECT p FROM Property p WHERE p.status = 'active' ORDER BY p.createdDate DESC")
    List<Property> findActivePropertiesOrderByCreatedDate();

    // Новинки: последние по createdDate, продвинутые первыми
    @Query("SELECT DISTINCT p FROM Property p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.realtor WHERE p.status = 'active' ORDER BY p.promoted DESC, p.createdDate DESC")
    List<Property> findLatestActive();

    // Популярные: условно дорогие (сортировка по цене убыв.), продвинутые первыми
    @Query("SELECT DISTINCT p FROM Property p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.realtor WHERE p.status = 'active' ORDER BY p.promoted DESC, p.price DESC")
    List<Property> findPopularActive();

    // Могут подойти: условно доступные (сортировка по цене возр.), продвинутые первыми
    @Query("SELECT DISTINCT p FROM Property p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.realtor WHERE p.status = 'active' ORDER BY p.promoted DESC, p.price ASC")
    List<Property> findAffordableActive();
    
    // Поиск с учетом продвижения
    @Query("SELECT p FROM Property p WHERE p.status = 'active' ORDER BY p.promoted DESC, p.createdDate DESC")
    List<Property> findActivePropertiesOrderByPromoted();
    
    // Загрузка property с user и realtor для чата
    @Query("SELECT DISTINCT p FROM Property p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.realtor WHERE p.id = :id")
    java.util.Optional<Property> findByIdWithUser(@Param("id") Long id);
}
