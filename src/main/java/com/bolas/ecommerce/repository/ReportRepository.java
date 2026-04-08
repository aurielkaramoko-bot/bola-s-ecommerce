package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByResolvedFalseOrderByCreatedAtAsc();

    List<Report> findByResolvedTrueOrderByCreatedAtDesc();

    long countByResolvedFalse();
}
