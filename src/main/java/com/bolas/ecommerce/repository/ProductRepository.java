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

    List<Product> findByAvailableTrueAndCategory_IdAndPriceCfaBetween(Long categoryId, Long min, Long max);

    List<Product> findTop6ByAvailableTrueOrderByFeaturedDescIdDesc();

    long countByCategory(Category category);

    /** Tous les produits d'un vendeur donné */
    List<Product> findByVendor(VendorUser vendor);

    /** Nombre de produits d'un vendeur (pour vérifier la limite plan GRATUIT) */
    long countByVendor(VendorUser vendor);
}
