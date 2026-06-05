package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.Category;
import com.bolas.ecommerce.model.Product;
import com.bolas.ecommerce.model.VendorUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /** 1. Accueil : Produits mis en avant */
    @Query("""
        SELECT p FROM Product p
        LEFT JOIN FETCH p.vendor v
        WHERE p.available = true
        AND (v IS NULL OR v.active = true)
        AND (v IS NULL OR p.featured = true OR v.plan = 'PREMIUM')
        ORDER BY
          CASE WHEN v IS NULL THEN 0
               WHEN v.plan = 'PREMIUM' THEN 1
               ELSE 2 END ASC,
          p.id DESC
        """)
    List<Product> findFeaturedForHomepage();

    /** 2. Accueil : Produits populaires */
    @Query("""
        SELECT p FROM Product p
        LEFT JOIN FETCH p.vendor v
        WHERE p.available = true
        AND (v IS NULL OR v.active = true)
        ORDER BY
          CASE WHEN v IS NULL THEN 0
               WHEN v.plan = 'PREMIUM' THEN 1
               ELSE 2 END ASC,
          p.featured DESC,
          p.id DESC
        LIMIT 6
        """)
    List<Product> findPopularForHomepage();


    /** 3. Recherche par mot-clé (Correction du crash Render) */
    @Query("""
        SELECT p FROM Product p
        WHERE p.available = true
        AND (p.vendor IS NULL OR p.vendor.active = true)
        AND (LOWER(p.name) LIKE LOWER(CONCAT('%',:q,'%'))
          OR LOWER(p.description) LIKE LOWER(CONCAT('%',:q,'%')))
        AND (:categoryId IS NULL OR p.category.id = :categoryId)
        ORDER BY p.sponsored DESC, p.id DESC
        """)
        List<Product> searchByKeyword(@Param("q") String q, @Param("categoryId") Long categoryId);

    /** 4. Méthodes utilisées par ProductController (Filtrage) */
    
    // Filtre par prix seul
    List<Product> findByAvailableTrueAndPriceCfaBetween(long min, long max);

    // Filtre par catégorie + prix (Correction demandée)
    @Query("""
        SELECT p FROM Product p 
        WHERE p.available = true
        AND (p.vendor IS NULL OR p.vendor.active = true)
        AND p.category.id = :categoryId
        AND p.priceCfa BETWEEN :min AND :max
        ORDER BY p.sponsored DESC, p.id DESC
        """)
    List<Product> findByAvailableTrueAndCategory_IdAndPriceCfaBetween(
        @Param("categoryId") Long categoryId, 
        @Param("min") long min, 
        @Param("max") long max
    );

    // Utilisé pour les carrousels ou listes rapides
    List<Product> findTop6ByAvailableTrueOrderByFeaturedDescIdDesc();

    /** --- Gestion Stock et Boutique --- */

    List<Product> findByVendorAndAvailableTrue(VendorUser vendor);
    long countByVendor(VendorUser vendor);
    List<Product> findByVendor(VendorUser vendor);
    List<Product> findByVendorOrderByIdDesc(VendorUser vendor);
    List<Product> findByAvailableTrue();

    @Query("""
        SELECT p FROM Product p
        LEFT JOIN FETCH p.category
        LEFT JOIN FETCH p.vendor v
        WHERE p.available = true
        AND (v IS NULL OR v.active = true)
        ORDER BY
          CASE WHEN v IS NULL THEN 1
               WHEN v.plan = 'PREMIUM' THEN 0
               ELSE 1 END ASC,
          p.sponsored DESC,
          p.id DESC
        """)
    List<Product> findAllAvailablePremiumFirst();

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.vendor ORDER BY p.id DESC")
    List<Product> findAllWithCategoryAndVendor();

    /** Fiche produit publique — eager fetch category + vendor pour éviter LazyInitializationException */
    @Query("""
        SELECT p FROM Product p
        LEFT JOIN FETCH p.category
        LEFT JOIN FETCH p.vendor
        WHERE p.id = :id
        """)
    java.util.Optional<Product> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.category = :category AND (p.vendor IS NULL OR p.vendor.active = true)")
    long countActiveByCategory(@Param("category") Category category);
    
    long countByCategory(Category category);

    /** Compteur optimisé pour la homepage (COUNT SQL, pas de chargement en mémoire) */
    long countByAvailableTrue();

    /** Produits en tendance actifs — utilisé par homepage et page /trends */
    @Query("""
        SELECT p FROM Product p
        LEFT JOIN FETCH p.vendor v
        WHERE p.available = true
        AND p.trendActive = true
        AND (v IS NULL OR (v.active = true AND v.vendorStatus = 'ACTIVE'))
        AND (p.trendExpiresAt IS NULL OR p.trendExpiresAt > CURRENT_TIMESTAMP)
        ORDER BY p.id DESC
        LIMIT 20
        """)
    List<Product> findCurrentlyTrendingAvailable();
}