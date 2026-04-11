package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.Category;
import com.bolas.ecommerce.model.Product;
import com.bolas.ecommerce.model.VendorUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** 1. Produits mis en avant (Featured) : Uniquement si le vendeur est actif */
    @Query("""
        SELECT p FROM Product p
        WHERE p.available = true
        AND (
          p.vendor IS NULL
          OR (p.vendor.active = true AND p.featured = true AND p.vendor.plan IN ('PRO','PRO_LOCAL','PREMIUM'))
        )
        ORDER BY
          CASE WHEN p.vendor IS NULL THEN 0
               WHEN p.vendor.plan = 'PREMIUM' THEN 1
               ELSE 2 END ASC,
          p.id DESC
        """)
    List<Product> findFeaturedForHomepage();

    /** 2. Produits populaires : Uniquement si le vendeur est actif */
    @Query("""
        SELECT p FROM Product p
        WHERE p.available = true
        AND (
          p.vendor IS NULL
          OR (p.vendor.active = true AND p.vendor.plan IN ('PRO','PRO_LOCAL','PREMIUM'))
        )
        ORDER BY
          CASE WHEN p.vendor IS NULL THEN 0
               WHEN p.vendor.plan = 'PREMIUM' THEN 1
               ELSE 2 END ASC,
          p.featured DESC,
          p.id DESC
        LIMIT 6
        """)
    List<Product> findPopularForHomepage();

    /** 3. Recherche par mot-clé : Sécurité totale sur le statut du vendeur */
    @Query("""
        SELECT p FROM Product p
        WHERE p.available = true
        AND p.vendor.active = true
        AND (LOWER(p.name) LIKE LOWER(CONCAT('%',:q,'%'))
          OR LOWER(p.description) LIKE LOWER(CONCAT('%',:q,'%')))
        ORDER BY p.sponsored DESC, p.id DESC
        """)
    List<Product> searchByKeyword(@Param("q") String q);

    /** 4. Recherche par mot-clé + Catégorie : Sécurité totale */
    @Query("""
        SELECT p FROM Product p
        WHERE p.available = true
        AND p.vendor.active = true
        AND p.category.id = :categoryId
        AND (LOWER(p.name) LIKE LOWER(CONCAT('%',:q,'%'))
          OR LOWER(p.description) LIKE LOWER(CONCAT('%',:q,'%')))
        ORDER BY p.sponsored DESC, p.id DESC
        """)
        List<Product> findByAvailableTrueAndCategoryIdAndPriceCfaBetween(Long categoryId, long min, long max);

        List<Product> findByAvailableTrueAndPriceCfaBetween(long min, long max);
        List<Product> searchByKeywordAndCategory(@Param("q") String q, @Param("categoryId") Long categoryId);

    /** 5. Filtrage par Catégorie et Prix : Sécurité totale */
    @Query("""
        SELECT p FROM Product p 
        WHERE p.available = true
        AND p.vendor.active = true
        AND p.category.id = :categoryId
        AND p.priceCfa BETWEEN :min AND :max
        ORDER BY p.sponsored DESC, p.id DESC
        """)
    List<Product> findByAvailableAndCategoryAndPriceRange(
        @Param("categoryId") Long categoryId,
        @Param("min") Long min,
        @Param("max") Long max);

    /** --- Méthodes de gestion de stock et boutique --- */

    // Utilisé pour la page publique d'une boutique (le contrôleur filtre déjà le vendor)
    List<Product> findByVendorAndAvailableTrue(VendorUser vendor);

    // Compte des produits pour les limites de plan (Gratuit/Pro)
    long countByVendor(VendorUser vendor);

    // Utile pour les statistiques par catégorie (Uniquement produits de vendeurs actifs)
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category = :category AND p.vendor.active = true")
    long countActiveByCategory(@Param("category") Category category);

    // Fallback simple pour les listes automatiques
    List<Product> findByAvailableTrueAndVendorActiveTrue();
    List<Product> findTop6ByAvailableTrueOrderByFeaturedDescIdDesc();

    // Pour l'AdminController qui veut le compte total par catégorie
long countByCategory(Category category);

// Pour le VendorController qui veut voir tous ses produits (stock etc.)
List<Product> findByVendor(VendorUser vendor);

// Pour les listes simples
List<Product> findByAvailableTrue();
List<Product> findByAvailableTrueAndCategory_IdAndPriceCfaBetween(Long categoryId, long min, long max);

}