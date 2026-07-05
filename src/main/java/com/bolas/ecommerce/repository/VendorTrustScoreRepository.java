package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.TrustLevel;
import com.bolas.ecommerce.model.VendorTrustScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour les scores de confiance vendeur.
 */
public interface VendorTrustScoreRepository extends JpaRepository<VendorTrustScore, Long> {

    /** Score d'un vendeur spécifique */
    Optional<VendorTrustScore> findByVendorId(Long vendorId);

    /** Vendeurs flaggés pour examen admin */
    @Query("""
        SELECT vts FROM VendorTrustScore vts
        JOIN FETCH vts.vendor v
        WHERE vts.flaggedForReview = true
        ORDER BY vts.score ASC
        """)
    List<VendorTrustScore> findFlaggedForReview();

    /** Nombre de vendeurs flaggés (pour badge dashboard admin) */
    @Query("SELECT COUNT(vts) FROM VendorTrustScore vts WHERE vts.flaggedForReview = true")
    long countFlaggedForReview();

    /** Classement par TrustLevel */
    List<VendorTrustScore> findByTrustLevelOrderByScoreDesc(TrustLevel trustLevel);

    /** Top vendeurs par score (pour affichage homepage) */
    @Query("""
        SELECT vts FROM VendorTrustScore vts
        JOIN FETCH vts.vendor v
        WHERE v.active = true
        AND v.vendorStatus = com.bolas.ecommerce.model.VendorStatus.ACTIVE
        ORDER BY vts.score DESC
        """)
    List<VendorTrustScore> findTopTrustedVendors();
}
