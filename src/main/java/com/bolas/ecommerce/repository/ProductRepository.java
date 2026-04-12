package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.Category;
import com.bolas.ecommerce.model.Product;
import com.bolas.ecommerce.model.VendorUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** 1. Produits mis en avant (Featured) - Accueil */
    @Query("""
        SELECT p FROM Product p
        WHERE p.available = true
        AND (p.vendor IS NULL OR p.vendor.active = true)
        AND (p.vendor IS NULL OR p.featured = true)
        ORDER BY
          CASE WHEN p.vendor IS NULL THEN 0
               WHEN p.vendor.plan = 'PREMIUM' THEN 1
               ELSE 2 END ASC,
          p.id DESC
        """)
    List<Product> findFeaturedForHomepage();

    /** 2. Produits populaires - Accueil */
    @Query("""
        SELECT p FROM Product p
        WHERE p.available = true
        AND (p.vendor IS NULL OR p.vendor.active = true)
        ORDER BY
          CASE WHEN p.vendor IS NULL THEN 0
               WHEN p.vendor.plan = 'PREMIUM' THEN 1
               ELSE 2 END ASC,
          p.featured DESC,
          p.id DESC
        LIMIT 6
        """)
    List<Product> findPopularForHomepage();

    /** 3. Recherche par mot-clé (Inclut Admin + Vendeurs actifs) */
    @Query("""
        SELECT p FROM Product p
        WHERE p.available = true
        AND (p.vendor IS NULL OR p.vendor.active = true)
        AND (LOWER(p.name) LIKE LOWER(CONCAT('%',:q,'%'))
          OR LOWER(p.description) LIKE LOWER(CONCAT('%',:q,'%')))
        ORDER BY p.sponsored DESC, p.id DESC
        """)
    List<Product> searchByKeyword(@Param("q") String q);

    /** 4. Recherche par mot-clé + Catégorie */
    @Query("""
        SELECT p FROM Product p
        WHERE p.available = true
        AND (p.vendor IS NULL OR p.vendor.active = true)
        AND p.category.id = :categoryId
        AND (LOWER(p.name) LIKE LOWER(CONCAT('%',:q,'%'))
          OR LOWER(p.description) LIKE LOWER(CONCAT('%',:q,'%')))
        ORDER BY p.sponsored DESC, p.id DESC
        """)
    List<Product> searchByKeywordAndCategory(@Param("q") String q, @Param("categoryId") Long categoryId);

    /** 5. Filtrage par Catégorie et Prix */
    @Query("""
        SELECT p FROM Product p 
        WHERE p.available = true
        AND (p.vendor IS NULL OR p.vendor.active = true)
        AND p.category.id = :categoryId
        AND p.priceCfa BETWEEN :min AND :max
        ORDER BY p.sponsored DESC, p.id DESC
        """)
    List<Product> findByAvailableTrueAndCategoryIdAndPriceCfaBetween(
        @Param("categoryId") Long categoryId, 
        @Param("min") long min, 
        @Param("max") long max
    );

    /** --- Autres méthodes de gestion --- */

    @Query("SELECT COUNT(p) FROM Product p WHERE p.category = :category AND (p.vendor IS NULL OR p.vendor.active = true)")
    long countActiveByCategory(@Param("category") Category category);

    List<Product> findByVendorAndAvailableTrue(VendorUser vendor);
    long countByVendor(VendorUser vendor);
    List<Product> findByVendor(VendorUser vendor);
    List<Product> findByAvailableTrue();
    
    // Pour l'admin : compte brut
    long countByCategory(Category category);
}