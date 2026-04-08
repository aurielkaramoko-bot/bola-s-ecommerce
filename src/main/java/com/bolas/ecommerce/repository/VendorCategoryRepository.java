package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.Category;
import com.bolas.ecommerce.model.VendorCategory;
import com.bolas.ecommerce.model.VendorUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VendorCategoryRepository extends JpaRepository<VendorCategory, Long> {

    List<VendorCategory> findByVendor(VendorUser vendor);

    /** Catégories autorisées pour un vendeur (IDs uniquement) */
    @Query("SELECT vc.category FROM VendorCategory vc WHERE vc.vendor = :vendor")
    List<Category> findCategoriesByVendor(VendorUser vendor);

    /** Vérifie si un vendeur a accès à une catégorie */
    boolean existsByVendorAndCategory(VendorUser vendor, Category category);

    void deleteByVendorAndCategory(VendorUser vendor, Category category);

    void deleteByVendor(VendorUser vendor);
}
