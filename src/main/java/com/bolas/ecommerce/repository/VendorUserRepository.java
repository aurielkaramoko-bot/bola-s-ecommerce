package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.VendorPlan;
import com.bolas.ecommerce.model.VendorStatus;
import com.bolas.ecommerce.model.VendorUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VendorUserRepository extends JpaRepository<VendorUser, Long> {

    Optional<VendorUser> findByUsername(String username);

    Optional<VendorUser> findByEmail(String email);

    List<VendorUser> findByVendorStatus(VendorStatus vendorStatus);

    List<VendorUser> findByActiveTrue();

    long countByVendorStatus(VendorStatus vendorStatus);

    /** Vendeurs PREMIUM actifs avec une bannière configurée */
    @Query("SELECT v FROM VendorUser v WHERE v.active = true AND v.plan = 'PREMIUM' AND v.bannerUrl IS NOT NULL")
    List<VendorUser> findActivePremiumWithBanner();

    /** Vendeurs actifs avec un statut spécifique (ex: ACTIVE uniquement) */
    List<VendorUser> findByVendorStatusAndActiveTrue(VendorStatus status);
}
