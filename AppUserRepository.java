package com.example.project2.repository;

import com.example.project2.model.AppUser;
import com.example.project2.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    
    List<AppUser> findByRole(UserRole role);
    
    @Query("SELECT u FROM AppUser u WHERE u.role = 'REALTOR'")
    List<AppUser> findRealtors();
    
    @Query("SELECT u FROM AppUser u WHERE u.realtorLicense IS NOT NULL")
    List<AppUser> findUsersWithLicense();
    
    @Query("SELECT u FROM AppUser u WHERE u.firstName LIKE %:name% OR u.lastName LIKE %:name%")
    List<AppUser> findByNameContaining(@Param("name") String name);
    
    boolean existsByEmail(String email);
}


