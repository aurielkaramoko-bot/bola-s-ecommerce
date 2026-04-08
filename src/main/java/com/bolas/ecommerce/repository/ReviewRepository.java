package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.Product;
import com.bolas.ecommerce.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProductAndApprovedTrueOrderByCreatedAtDesc(Product product);

    List<Review> findByApprovedFalseOrderByCreatedAtAsc();

    long countByProductAndApprovedTrue(Product product);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.product = :product AND r.approved = true")
    double averageRatingByProduct(Product product);
}
