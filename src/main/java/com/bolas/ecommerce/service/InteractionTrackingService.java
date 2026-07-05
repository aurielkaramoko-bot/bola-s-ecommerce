package com.bolas.ecommerce.service;

import com.bolas.ecommerce.model.CustomerProductInteraction;
import com.bolas.ecommerce.model.InteractionType;
import com.bolas.ecommerce.model.Product;
import com.bolas.ecommerce.repository.CustomerProductInteractionRepository;
import com.bolas.ecommerce.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Service asynchrone pour enregistrer les interactions client-produit
 * sans bloquer l'expérience utilisateur.
 *
 * Déduplique les vues : max 1 VIEW par produit par session de 30 minutes.
 */
@Service
public class InteractionTrackingService {

    private static final Logger log = LoggerFactory.getLogger(InteractionTrackingService.class);

    /** Durée minimale entre deux VIEW du même produit par le même client */
    private static final long VIEW_DEDUP_MINUTES = 30;

    private final CustomerProductInteractionRepository interactionRepository;
    private final ProductRepository productRepository;

    public InteractionTrackingService(CustomerProductInteractionRepository interactionRepository,
                                       ProductRepository productRepository) {
        this.interactionRepository = interactionRepository;
        this.productRepository = productRepository;
    }

    /**
     * Enregistre une vue de produit (dédupliquée sur 30 min).
     */
    @Async
    @Transactional
    public void trackView(Long customerId, Long productId) {
        if (customerId == null || productId == null) return;

        try {
            // Anti-doublon : pas de re-enregistrement dans les 30 dernières minutes
            Instant threshold = Instant.now().minus(VIEW_DEDUP_MINUTES, ChronoUnit.MINUTES);
            if (interactionRepository.existsRecentInteraction(
                    customerId, productId, InteractionType.VIEW, threshold)) {
                return;
            }

            saveInteraction(customerId, productId, InteractionType.VIEW);
            log.debug("IA Tracking: VIEW client={} produit={}", customerId, productId);
        } catch (Exception e) {
            log.warn("Erreur tracking VIEW: {}", e.getMessage());
        }
    }

    /**
     * Enregistre un ajout au panier.
     */
    @Async
    @Transactional
    public void trackAddToCart(Long customerId, Long productId) {
        if (customerId == null || productId == null) return;

        try {
            saveInteraction(customerId, productId, InteractionType.ADD_TO_CART);
            log.debug("IA Tracking: ADD_TO_CART client={} produit={}", customerId, productId);
        } catch (Exception e) {
            log.warn("Erreur tracking ADD_TO_CART: {}", e.getMessage());
        }
    }

    /**
     * Enregistre un achat confirmé.
     */
    @Async
    @Transactional
    public void trackPurchase(Long customerId, Long productId) {
        if (customerId == null || productId == null) return;

        try {
            saveInteraction(customerId, productId, InteractionType.PURCHASE);
            log.debug("IA Tracking: PURCHASE client={} produit={}", customerId, productId);
        } catch (Exception e) {
            log.warn("Erreur tracking PURCHASE: {}", e.getMessage());
        }
    }

    /**
     * Enregistre un avis laissé.
     */
    @Async
    @Transactional
    public void trackReview(Long customerId, Long productId) {
        if (customerId == null || productId == null) return;

        try {
            saveInteraction(customerId, productId, InteractionType.REVIEW);
            log.debug("IA Tracking: REVIEW client={} produit={}", customerId, productId);
        } catch (Exception e) {
            log.warn("Erreur tracking REVIEW: {}", e.getMessage());
        }
    }

    // ─── Private ────────────────────────────────────────────────────────────

    private void saveInteraction(Long customerId, Long productId, InteractionType type) {
        Long categoryId = null;
        Long vendorId = null;

        // Dénormaliser catégorie et vendeur pour des requêtes de recommandation rapides
        try {
            Product product = productRepository.findById(productId).orElse(null);
            if (product != null) {
                if (product.getCategory() != null) {
                    categoryId = product.getCategory().getId();
                }
                if (product.getVendor() != null) {
                    vendorId = product.getVendor().getId();
                }
            }
        } catch (Exception e) {
            // Pas critique — l'interaction sera enregistrée sans dénormalisation
            log.debug("Impossible de dénormaliser produit {}: {}", productId, e.getMessage());
        }

        CustomerProductInteraction interaction = new CustomerProductInteraction(
                customerId, productId, type, categoryId, vendorId);
        interactionRepository.save(interaction);
    }
}
