package com.example.project2.repository;

import com.example.project2.model.AdminLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {
    @Query("SELECT l FROM AdminLog l ORDER BY l.createdAt DESC")
    List<AdminLog> findLatest();

    @Query("SELECT l FROM AdminLog l WHERE l.userEmail = :email ORDER BY l.createdAt DESC")
    List<AdminLog> findByUserEmail(@Param("email") String email);

    @Query("SELECT l FROM AdminLog l\n            WHERE l.userEmail = COALESCE(:email, l.userEmail)\n              AND l.action = COALESCE(:action, l.action)\n              AND l.createdAt >= COALESCE(:fromDate, l.createdAt)\n              AND l.createdAt <= COALESCE(:toDate, l.createdAt)\n            ORDER BY l.createdAt DESC")
    Page<AdminLog> search(@Param("email") String email,
                          @Param("action") String action,
                          @Param("fromDate") LocalDateTime fromDate,
                          @Param("toDate") LocalDateTime toDate,
                          Pageable pageable);
}
