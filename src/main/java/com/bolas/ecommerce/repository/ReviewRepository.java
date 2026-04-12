package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.Product;
import com.bolas.ecommerce.model.Review;
import com.bolas.ecommerce.model.VendorUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProductAndApprovedTrueOrderByCreatedAtDesc(Product product);

    List<Review> findByApprovedFalseOrderByCreatedAtAsc();

    long countByProductAndApprovedTrue(Product product);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.product = :product AND r.approved = true")
    double averageRatingByProduct(@Param("product") Product product);

    /** Avis approuvés pour tous les produits d'un vendeur */
    @Query("SELECT r FROM Review r WHERE r.product.vendor = :vendor AND r.approved = true ORDER BY r.createdAt DESC")
    List<Review> findApprovedByVendor(@Param("vendor") VendorUser vendor);

    /** Note moyenne de tous les produits d'un vendeur */
    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.product.vendor = :vendor AND r.approved = true")
    double averageRatingByVendor(@Param("vendor") VendorUser vendor);

    /** Nombre total d'avis approuvés pour un vendeur */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.vendor = :vendor AND r.approved = true")
    long countApprovedByVendor(@Param("vendor") VendorUser vendor);
}

