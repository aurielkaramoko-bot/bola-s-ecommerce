package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.CustomerProductInteraction;
import com.bolas.ecommerce.model.InteractionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Repository pour les interactions client-produit.
 * Requêtes optimisées pour le moteur de recommandation IA.
 */
public interface CustomerProductInteractionRepository
        extends JpaRepository<CustomerProductInteraction, Long> {

    /** Toutes les interactions d'un client, triées par date décroissante */
    List<CustomerProductInteraction> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    /** Interactions récentes d'un client (pour le profil d'intérêt) */
    @Query("""
        SELECT cpi FROM CustomerProductInteraction cpi
        WHERE cpi.customerId = :customerId
        AND cpi.createdAt >= :since
        ORDER BY cpi.createdAt DESC
        """)
    List<CustomerProductInteraction> findRecentByCustomer(
            @Param("customerId") Long customerId,
            @Param("since") Instant since);

    /**
     * Top catégories d'un client par somme de poids (préférences).
     * Retourne des Object[] : [categoryId (Long), totalWeight (Long)]
     */
    @Query("""
        SELECT cpi.categoryId, SUM(cpi.weight)
        FROM CustomerProductInteraction cpi
        WHERE cpi.customerId = :customerId
        AND cpi.categoryId IS NOT NULL
        GROUP BY cpi.categoryId
        ORDER BY SUM(cpi.weight) DESC
        """)
    List<Object[]> findTopCategoriesByCustomer(@Param("customerId") Long customerId);

    /**
     * Filtrage collaboratif : produits achetés par des clients qui ont aussi acheté productId.
     * "Les clients qui ont acheté X ont aussi acheté Y".
     * Retourne des Object[] : [productId (Long), coCount (Long)]
     */
    @Query("""
        SELECT other.productId, COUNT(other.productId)
        FROM CustomerProductInteraction other
        WHERE other.interactionType IN ('PURCHASE', 'ADD_TO_CART')
        AND other.productId <> :productId
        AND other.customerId IN (
            SELECT cpi.customerId FROM CustomerProductInteraction cpi
            WHERE cpi.productId = :productId
            AND cpi.interactionType IN ('PURCHASE', 'ADD_TO_CART')
        )
        GROUP BY other.productId
        ORDER BY COUNT(other.productId) DESC
        """)
    List<Object[]> findCoOccurrenceProducts(@Param("productId") Long productId);

    /**
     * Produits populaires dans le même pays que le vendeur (via vendorId dénormalisé).
     * Retourne des Object[] : [productId (Long), totalWeight (Long)]
     */
    @Query(value = """
        SELECT cpi.product_id, SUM(cpi.weight) AS total_weight
        FROM customer_product_interactions cpi
        INNER JOIN products p ON p.id = cpi.product_id
        LEFT JOIN vendor_users v ON v.id = p.vendor_id
        WHERE (v.shop_country = :countryCode OR v.id IS NULL)
        AND p.available = true
        AND cpi.created_at >= :since
        GROUP BY cpi.product_id
        ORDER BY total_weight DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findPopularByCountry(@Param("countryCode") String countryCode,
                                         @Param("since") Instant since,
                                         @Param("limit") int limit);

    /** Vérifie si une interaction récente existe (anti-doublon pour les vues) */
    @Query("""
        SELECT COUNT(cpi) > 0 FROM CustomerProductInteraction cpi
        WHERE cpi.customerId = :customerId
        AND cpi.productId = :productId
        AND cpi.interactionType = :type
        AND cpi.createdAt >= :since
        """)
    boolean existsRecentInteraction(@Param("customerId") Long customerId,
                                     @Param("productId") Long productId,
                                     @Param("type") InteractionType type,
                                     @Param("since") Instant since);

    /** Produits avec lesquels le client a interagi (pour exclusion des recommandations) */
    @Query("""
        SELECT DISTINCT cpi.productId FROM CustomerProductInteraction cpi
        WHERE cpi.customerId = :customerId
        AND cpi.interactionType = 'PURCHASE'
        """)
    List<Long> findPurchasedProductIds(@Param("customerId") Long customerId);

    /** Nombre total d'interactions (pour les stats admin) */
    long countByCustomerId(Long customerId);
}
