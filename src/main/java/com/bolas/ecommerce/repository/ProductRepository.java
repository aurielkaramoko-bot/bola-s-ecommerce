package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.Category;
import com.bolas.ecommerce.model.Product;
import com.bolas.ecommerce.model.VendorUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByAvailableTrueAndFeaturedTrue();

    List<Product> findByAvailableTrue();

    List<Product> findByAvailableTrueAndCategory(Category category);

    List<Product> findByAvailableTrueAndCategory_Id(Long categoryId);

    List<Product> findByAvailableTrueAndPriceCfaBetween(Long min, Long max);

    @org.springframework.data.jpa.repository.Query("""
        SELECT p FROM Product p WHERE p.available = true
        AND p.category.id = :categoryId
        AND p.priceCfa BETWEEN :min AND :max
        ORDER BY p.sponsored DESC, p.id DESC
        """)
    List<Product> findByAvailableTrueAndCategory_IdAndPriceCfaBetween(
        @org.springframework.data.repository.query.Param("categoryId") Long categoryId,
        @org.springframework.data.repository.query.Param("min") Long min,
        @org.springframework.data.repository.query.Param("max") Long max);

    List<Product> findTop6ByAvailableTrueOrderByFeaturedDescIdDesc();

    long countByCategory(Category category);

    /** Tous les produits d'un vendeur donné */
    List<Product> findByVendor(VendorUser vendor);

    /** Produits disponibles d'un vendeur (page boutique publique) */
    List<Product> findByVendorAndAvailableTrue(VendorUser vendor);

    /** Nombre de produits d'un vendeur (pour vérifier la limite plan GRATUIT) */
    long countByVendor(VendorUser vendor);

    /** Recherche par mot-clé dans le nom ou la description — sponsorisés PREMIUM en tête */
    @org.springframework.data.jpa.repository.Query("""
        SELECT p FROM Product p
        WHERE p.available = true
        AND (LOWER(p.name) LIKE LOWER(CONCAT('%',:q,'%'))
          OR LOWER(p.description) LIKE LOWER(CONCAT('%',:q,'%')))
        ORDER BY p.sponsored DESC, p.id DESC
        """)
    List<Product> searchByKeyword(@org.springframework.data.repository.query.Param("q") String q);

    /** Recherche par mot-clé + catégorie — sponsorisés PREMIUM en tête */
    @org.springframework.data.jpa.repository.Query("""
        SELECT p FROM Product p
        WHERE p.available = true
        AND p.category.id = :categoryId
        AND (LOWER(p.name) LIKE LOWER(CONCAT('%',:q,'%'))
          OR LOWER(p.description) LIKE LOWER(CONCAT('%',:q,'%')))
        ORDER BY p.sponsored DESC, p.id DESC
        """)
    List<Product> searchByKeywordAndCategory(
        @org.springframework.data.repository.query.Param("q") String q,
        @org.springframework.data.repository.query.Param("categoryId") Long categoryId);

    /** Produits mis en avant UNIQUEMENT pour vendeurs PRO/PRO_LOCAL/PREMIUM (homepage)
     *  + tous les produits admin (vendor IS NULL) disponibles
     *  PREMIUM apparaît en premier, puis PRO_LOCAL/PRO, triés par featured puis id */
    @org.springframework.data.jpa.repository.Query("""
        SELECT p FROM Product p
        WHERE p.available = true
        AND (
          p.vendor IS NULL
          OR (p.featured = true AND p.vendor.plan IN (
            com.bolas.ecommerce.model.VendorPlan.PRO,
            com.bolas.ecommerce.model.VendorPlan.PRO_LOCAL,
            com.bolas.ecommerce.model.VendorPlan.PREMIUM))
        )
        ORDER BY
          CASE WHEN p.vendor IS NULL THEN 0
               WHEN p.vendor.plan = com.bolas.ecommerce.model.VendorPlan.PREMIUM THEN 1
               ELSE 2 END ASC,
          p.id DESC
        """)
    List<Product> findFeaturedForHomepage();

    /** Articles populaires UNIQUEMENT pour vendeurs PRO/PRO_LOCAL/PREMIUM (homepage)
     *  + tous les produits admin (vendor IS NULL)
     *  PREMIUM apparaît en premier */
    @org.springframework.data.jpa.repository.Query("""
        SELECT p FROM Product p
        WHERE p.available = true
        AND (
          p.vendor IS NULL
          OR p.vendor.plan IN (
            com.bolas.ecommerce.model.VendorPlan.PRO,
            com.bolas.ecommerce.model.VendorPlan.PRO_LOCAL,
            com.bolas.ecommerce.model.VendorPlan.PREMIUM)
        )
        ORDER BY
          CASE WHEN p.vendor IS NULL THEN 0
               WHEN p.vendor.plan = com.bolas.ecommerce.model.VendorPlan.PREMIUM THEN 1
               ELSE 2 END ASC,
          p.featured DESC,
          p.id DESC
        LIMIT 6
        """)
    List<Product> findPopularForHomepage();
}
