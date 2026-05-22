package com.bolas.ecommerce.service;

import com.bolas.ecommerce.model.VendorPlan;
import com.bolas.ecommerce.model.VendorStatus;
import com.bolas.ecommerce.model.VendorUser;
import com.bolas.ecommerce.repository.ProductRepository;
import com.bolas.ecommerce.repository.VendorUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Vérifie toutes les 60 secondes si des abonnements vendeurs ont expiré.
 * Si un abonnement expire à la minute exacte, le vendeur est rétrogradé en GRATUIT.
 */
@Service
public class SubscriptionExpiryService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionExpiryService.class);

    private final VendorUserRepository vendorUserRepository;
    private final ProductRepository productRepository;

    public SubscriptionExpiryService(VendorUserRepository vendorUserRepository,
                                      ProductRepository productRepository) {
        this.vendorUserRepository = vendorUserRepository;
        this.productRepository = productRepository;
    }

    /**
     * Vérifie toutes les 60 secondes les abonnements expirés.
     * Rétrograde les vendeurs dont l'abonnement est passé en GRATUIT.
     */
    @Scheduled(fixedDelay = 60_000) // toutes les 60 secondes
    @Transactional
    public void checkExpiredSubscriptions() {
        LocalDateTime now = LocalDateTime.now();

        List<VendorUser> expired = vendorUserRepository
                .findBySubscriptionExpiresAtNotNullAndSubscriptionExpiresAtBeforeOrderBySubscriptionExpiresAtAsc(now)
                .stream()
                .filter(v -> v.getPlan() != VendorPlan.GRATUIT && v.isActive())
                .toList();

        if (expired.isEmpty()) return;

        for (VendorUser vendor : expired) {
            log.info("⏰ Abonnement expiré pour {} ({}), rétrogradation en GRATUIT",
                    vendor.getDisplayName(), vendor.getSubscriptionExpiresAt());

            // Rétrogradation en GRATUIT
            vendor.setPlan(VendorPlan.GRATUIT);

            // Masquer les produits au-delà de la limite GRATUIT (10 max)
            var products = productRepository.findByVendor(vendor);
            if (products.size() > 10) {
                products.stream().skip(10).forEach(p -> {
                    p.setAvailable(false);
                    productRepository.save(p);
                });
                log.info("   → {} produits masqués (limite GRATUIT)", products.size() - 10);
            }

            vendorUserRepository.save(vendor);
        }

        if (!expired.isEmpty()) {
            log.info("✅ {} abonnement(s) expiré(s) traité(s)", expired.size());
        }
    }
}
