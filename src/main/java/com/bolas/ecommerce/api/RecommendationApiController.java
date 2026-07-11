package com.bolas.ecommerce.api;

import com.bolas.ecommerce.dto.InteractionDto;
import com.bolas.ecommerce.model.Product;
import com.bolas.ecommerce.service.InteractionTrackingService;
import com.bolas.ecommerce.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API REST pour le moteur de recommandation IA.
 *
 * - GET  /api/recommendations → Produits recommandés pour un client
 * - GET  /api/recommendations/similar/{productId} → Produits similaires
 * - POST /api/interactions → Enregistrer une interaction (VIEW, ADD_TO_CART)
 */
@RestController
@RequestMapping("/api")
public class RecommendationApiController {

    private final RecommendationService recommendationService;
    private final InteractionTrackingService trackingService;

    public RecommendationApiController(RecommendationService recommendationService,
                                        InteractionTrackingService trackingService) {
        this.recommendationService = recommendationService;
        this.trackingService = trackingService;
    }

    /**
     * Recommandations personnalisées pour un client.
     */
    @GetMapping("/recommendations")
    public ResponseEntity<List<Map<String, Object>>> getRecommendations(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false, defaultValue = "TG") String country,
            @RequestParam(required = false, defaultValue = "8") int limit) {

        List<Product> recommendations = recommendationService.getRecommendations(customerId, country, limit);
        return ResponseEntity.ok(toProductMaps(recommendations));
    }

    /**
     * Produits similaires (pour la fiche produit "Vous aimerez aussi").
     */
    @GetMapping("/recommendations/similar/{productId}")
    public ResponseEntity<List<Map<String, Object>>> getSimilarProducts(
            @PathVariable Long productId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false, defaultValue = "6") int limit) {

        List<Product> similar = recommendationService.getSimilarProducts(productId, customerId, limit);
        return ResponseEntity.ok(toProductMaps(similar));
    }

    /**
     * Enregistrer une interaction client-produit (appelé par le JS frontend).
     */
    @PostMapping("/interactions")
    public ResponseEntity<Map<String, String>> trackInteraction(
            @Valid @RequestBody InteractionDto body) {

        Long customerId = body.getCustomerId();
        Long productId  = body.getProductId();
        String type     = body.getType();

        switch (type.toUpperCase()) {
            case "VIEW"        -> trackingService.trackView(customerId, productId);
            case "ADD_TO_CART" -> trackingService.trackAddToCart(customerId, productId);
            case "PURCHASE"    -> trackingService.trackPurchase(customerId, productId);
            case "REVIEW"      -> trackingService.trackReview(customerId, productId);
            default            -> { return ResponseEntity.badRequest().body(Map.of("error", "Type inconnu: " + type)); }
        }

        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    // ─── Private ────────────────────────────────────────────────────────────

    private List<Map<String, Object>> toProductMaps(List<Product> products) {
        return products.stream().map(p -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("priceCfa", p.getEffectivePriceCfa());
            map.put("originalPriceCfa", p.getPriceCfa());
            map.put("onPromotion", p.isOnPromotion());
            map.put("imageUrl", p.getImageUrl());
            map.put("available", p.isAvailable());
            map.put("trending", p.isCurrentlyTrending());
            if (p.getCategory() != null) {
                map.put("categoryName", p.getCategory().getName());
            }
            if (p.getVendor() != null) {
                map.put("vendorName", p.getVendor().getDisplayName());
                map.put("vendorId", p.getVendor().getId());
            }
            return map;
        }).collect(Collectors.toList());
    }
}
