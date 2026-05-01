package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.ShopSeller;
import com.bolas.ecommerce.model.VendorUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShopSellerRepository extends JpaRepository<ShopSeller, Long> {

    /** Tous les sous-vendeurs d'une boutique */
    List<ShopSeller> findByVendorOrderByCreatedAtDesc(VendorUser vendor);

    /** Sous-vendeurs actifs d'une boutique */
    List<ShopSeller> findByVendorAndActiveTrue(VendorUser vendor);

    /** Recherche par identifiant (pour login) */
    Optional<ShopSeller> findByUsername(String username);

    /** Compte les sous-vendeurs d'une boutique (pour limiter par plan) */
    long countByVendor(VendorUser vendor);

    /** Supprime tous les sous-vendeurs d'une boutique (cascade) */
    void deleteByVendor(VendorUser vendor);
}
