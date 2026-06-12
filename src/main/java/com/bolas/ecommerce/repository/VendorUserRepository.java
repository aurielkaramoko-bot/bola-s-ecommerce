package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.VendorPlan;
import com.bolas.ecommerce.model.VendorStatus;
import com.bolas.ecommerce.model.VendorUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    /** Vendeurs PRO actifs avec une bannière configurée */
    @Query("SELECT v FROM VendorUser v WHERE v.active = true AND v.plan IN ('PRO', 'PRO_LOCAL') AND v.bannerUrl IS NOT NULL")
    List<VendorUser> findActiveProWithBanner();

    /** Vendeurs actifs avec un statut spécifique (ex: ACTIVE uniquement) */
    List<VendorUser> findByVendorStatusAndActiveTrue(VendorStatus status);

    /** Compteur optimisé — évite de charger tous les vendeurs en mémoire */
    long countByVendorStatusAndActiveTrue(VendorStatus status);

    /** Vendeurs dont l'abonnement expire dans une plage de dates */
    @Query("SELECT v FROM VendorUser v WHERE v.subscriptionExpiresAt >= :startDate AND v.subscriptionExpiresAt <= :endDate AND v.active = true")
    List<VendorUser> findBySubscriptionExpiresAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /** Vendeurs dont l'abonnement est expiré (pour le service d'expiration) */
    @Query("SELECT v FROM VendorUser v WHERE v.subscriptionExpiresAt IS NOT NULL AND v.subscriptionExpiresAt < :now AND v.active = true")
    List<VendorUser> findExpiredSubscriptions(@Param("now") LocalDateTime now);

    /** Compte les vendeurs par plan (analytics optimisé) */
    long countByPlan(VendorPlan plan);

    /** Parrainage : cherche un vendeur par son code de parrainage */
    Optional<VendorUser> findByReferralCode(String referralCode);

    /** Parrainage : vérifie si un code est déjà utilisé */
    boolean existsByReferralCode(String referralCode);

    /** Parrainage : filleuls d'un parrain donné */
    List<VendorUser> findByReferredById(Long referredById);

    /** Abonnements en attente de validation admin */
    @Query("SELECT v FROM VendorUser v WHERE v.pendingPlan IS NOT NULL AND v.pendingPlan <> '' ORDER BY v.pendingPlanRequestedAt ASC")
    List<VendorUser> findPendingSubscriptions();

    /** Nombre de demandes d'abonnement en attente (pour badge dashboard admin) */
    @Query("SELECT COUNT(v) FROM VendorUser v WHERE v.pendingPlan IS NOT NULL AND v.pendingPlan <> ''")
    long countPendingSubscriptions();

    /**
     * Recherche filtrable pour la page /boutiques.
     * Tous les paramètres sont optionnels : null = pas de filtre sur ce champ.
     */
    @Query("""
        SELECT v FROM VendorUser v
        WHERE v.active = true
          AND v.vendorStatus = com.bolas.ecommerce.model.VendorStatus.ACTIVE
          AND (:q IS NULL OR LOWER(v.shopName) LIKE LOWER(CONCAT('%', :q, '%'))
                          OR LOWER(v.shopDescription) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:country IS NULL OR v.shopCountry = :country)
          AND (:plan IS NULL OR CAST(v.plan AS string) = :plan)
        ORDER BY
          CASE v.plan WHEN 'PREMIUM' THEN 0 WHEN 'PRO' THEN 1 WHEN 'PRO_LOCAL' THEN 2 ELSE 3 END,
          v.shopName ASC
        """)
    List<VendorUser> searchBoutiques(@Param("q") String q,
                                     @Param("country") String country,
                                     @Param("plan") String plan);
}

