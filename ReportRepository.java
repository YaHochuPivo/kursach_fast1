package com.example.project2.repository;

import com.example.project2.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    
    @Query("SELECT r FROM Report r WHERE r.user.id = :userId")
    List<Report> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT r FROM Report r WHERE r.reportType = :reportType")
    List<Report> findByReportType(@Param("reportType") String reportType);
    
    @Query("SELECT r FROM Report r WHERE r.user.id = :userId AND r.reportType = :reportType")
    List<Report> findByUserIdAndReportType(@Param("userId") Long userId, @Param("reportType") String reportType);
}
