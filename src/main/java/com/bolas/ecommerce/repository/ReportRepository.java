package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByResolvedFalseOrderByCreatedAtAsc();

    List<Report> findByResolvedTrueOrderByCreatedAtDesc();

    long countByResolvedFalse();

    /** Nombre de signalements ciblant un vendeur (pour le TrustScore) */
    @Query("SELECT COUNT(r) FROM Report r WHERE r.targetType = 'VENDOR' AND r.targetId = :vendorId")
    long countByVendorId(@Param("vendorId") Long vendorId);
}
