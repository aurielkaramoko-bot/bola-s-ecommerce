package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.Product;
import com.bolas.ecommerce.model.ProductComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductCommentRepository extends JpaRepository<ProductComment, Long> {

    /** Commentaires racines (non réponses) */
    @Query("SELECT c FROM ProductComment c WHERE c.product = :product AND c.parentId IS NULL ORDER BY c.createdAt DESC")
    List<ProductComment> findRootByProduct(@Param("product") Product product);

    /** Réponses d'un commentaire */
    @Query("SELECT c FROM ProductComment c WHERE c.parentId = :parentId ORDER BY c.createdAt ASC")
    List<ProductComment> findReplies(@Param("parentId") Long parentId);

    /** Compte total de commentaires (racines + réponses) */
    long countByProduct(Product product);

    /** Compte racines uniquement */
    @Query("SELECT COUNT(c) FROM ProductComment c WHERE c.product = :product AND c.parentId IS NULL")
    long countRootByProduct(@Param("product") Product product);

    /** Like : incrément atomique */
    @Modifying
    @Query("UPDATE ProductComment c SET c.likesCount = c.likesCount + 1 WHERE c.id = :id")
    void incrementLikes(@Param("id") Long id);

    /** A déjà liké ? (table séparée optionnelle — on fait simple côté client) */
    @Query("SELECT c FROM ProductComment c WHERE c.product.id = :productId ORDER BY c.createdAt DESC")
    List<ProductComment> findAllByProductId(@Param("productId") Long productId);
}
