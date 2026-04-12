package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.VendorPlan;
import com.bolas.ecommerce.model.VendorStatus;
import com.bolas.ecommerce.model.VendorUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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

    /** Vendeurs dont l'abonnement expire à une date spécifique */
    List<VendorUser> findBySubscriptionExpiresAt(LocalDate date);

    /** Vendeurs dont l'abonnement expire dans une plage de dates */
    @Query("SELECT v FROM VendorUser v WHERE v.subscriptionExpiresAt >= :startDate AND v.subscriptionExpiresAt <= :endDate AND v.active = true")
    List<VendorUser> findBySubscriptionExpiresAtBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    List<VendorUser> findBySubscriptionExpiresAtNotNullAndSubscriptionExpiresAtBeforeOrderBySubscriptionExpiresAtAsc(java.time.LocalDate date);
}
