package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.LoyaltyCard;
import com.bolas.ecommerce.model.VendorUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoyaltyCardRepository extends JpaRepository<LoyaltyCard, Long> {

    /** Toutes les cartes d'un vendeur, les actives en premier */
    List<LoyaltyCard> findByVendorOrderByActiveDescCreatedAtDesc(VendorUser vendor);

    /** Recherche par code (pour validation lors d'une commande) */
    Optional<LoyaltyCard> findByCode(String code);

    /** Recherche par téléphone client + vendeur */
    Optional<LoyaltyCard> findByVendorAndCustomerPhone(VendorUser vendor, String customerPhone);

    /** Compte des cartes actives d'un vendeur */
    long countByVendorAndActiveTrue(VendorUser vendor);
}
