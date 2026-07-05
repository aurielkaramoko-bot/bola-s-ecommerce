package com.bolas.ecommerce.service;

import com.bolas.ecommerce.model.Product;
import com.bolas.ecommerce.model.VendorStatus;
import com.bolas.ecommerce.repository.CustomerProductInteractionRepository;
import com.bolas.ecommerce.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Moteur de recommandation IA pour Bola's Boutique.
 *
 * Trois couches de recommandation combinées, adaptées au contexte ouest-africain
 * où la découverte de produits est un vrai problème (pas de Google Shopping,
 * pas d'algos occidentaux adaptés) :
 *
 *  1. Filtrage collaboratif : "Les clients qui ont acheté X ont aussi acheté Y"
 *  2. Content-based : Similarité par catégorie, vendeur, gamme de prix
 *  3. Contextuel : Même pays, promotions actives, tendances locales
 *
 * Fallback intelligent pour les nouveaux clients (cold start).
 */
@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    /** Période de référence pour les interactions récentes */
    private static final int RECENT_DAYS = 90;

    /** Poids des couches de recommandation */
    private static final double COLLABORATIVE_WEIGHT = 0.40;
    private static final double CONTENT_WEIGHT = 0.35;
    private static final double CONTEXTUAL_WEIGHT = 0.25;

    private final CustomerProductInteractionRepository interactionRepo;
    private final ProductRepository productRepository;

    public RecommendationService(CustomerProductInteractionRepository interactionRepo,
                                  ProductRepository productRepository) {
        this.interactionRepo = interactionRepo;
        this.productRepository = productRepository;
    }

    /**
     * Point d'entrée principal : retourne les N meilleurs produits recommandés.
     *
     * @param customerId  ID du client (null → cold start)
     * @param countryCode Code pays ISO 2 (ex: "TG")
     * @param limit       Nombre de recommandations à retourner
     * @return Liste de produits recommandés, triés par score décroissant
     */
    @Transactional(readOnly = true)
    public List<Product> getRecommendations(Long customerId, String countryCode, int limit) {
        try {
            if (customerId == null) {
                return getColdStartRecommendations(countryCode, limit);
            }

            // Vérifier si le client a assez d'interactions pour le filtrage personnalisé
            long interactionCount = interactionRepo.countByCustomerId(customerId);
            if (interactionCount < 3) {
                return getColdStartRecommendations(countryCode, limit);
            }

            return getPersonalizedRecommendations(customerId, countryCode, limit);
        } catch (Exception e) {
            log.error("Erreur moteur de recommandation IA", e);
            return getFallbackRecommendations(limit);
        }
    }

    /**
     * Recommandations pour la fiche produit : "Vous aimerez aussi".
     */
    @Transactional(readOnly = true)
    public List<Product> getSimilarProducts(Long productId, Long customerId, int limit) {
        try {
            Map<Long, Double> scores = new LinkedHashMap<>();

            // 1. Filtrage collaboratif : co-occurrence
            List<Object[]> coProducts = interactionRepo.findCoOccurrenceProducts(productId);
            double maxCo = coProducts.isEmpty() ? 1 : ((Number) coProducts.get(0)[1]).doubleValue();
            for (Object[] row : coProducts) {
                Long pId = ((Number) row[0]).longValue();
                double coScore = ((Number) row[1]).doubleValue() / maxCo;
                scores.merge(pId, coScore * COLLABORATIVE_WEIGHT, Double::sum);
            }

            // 2. Content-based : même catégorie et gamme de prix
            Product sourceProduct = productRepository.findById(productId).orElse(null);
            if (sourceProduct != null) {
                addContentBasedScores(scores, sourceProduct);
            }

            // 3. Exclure le produit source et les produits déjà achetés
            scores.remove(productId);
            if (customerId != null) {
                List<Long> purchased = interactionRepo.findPurchasedProductIds(customerId);
                purchased.forEach(scores::remove);
            }

            // Charger et filtrer les produits valides
            return resolveAndFilter(scores, limit);
        } catch (Exception e) {
            log.error("Erreur recommandations similaires pour produit {}", productId, e);
            return getFallbackRecommendations(limit);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  COUCHES DE RECOMMANDATION
    // ═══════════════════════════════════════════════════════════════════════

    private List<Product> getPersonalizedRecommendations(Long customerId, String countryCode, int limit) {
        Map<Long, Double> scores = new LinkedHashMap<>();

        // ── Couche 1 : Filtrage collaboratif ──────────────────────────────
        // Pour chaque produit acheté/panier, trouver les co-occurrences
        List<Long> interactedIds = interactionRepo.findPurchasedProductIds(customerId);
        Set<Long> excludeSet = new HashSet<>(interactedIds);

        for (Long pid : interactedIds.stream().limit(10).toList()) {
            List<Object[]> coProducts = interactionRepo.findCoOccurrenceProducts(pid);
            for (Object[] row : coProducts) {
                Long coId = ((Number) row[0]).longValue();
                if (excludeSet.contains(coId)) continue;
                double coScore = ((Number) row[1]).doubleValue();
                scores.merge(coId, coScore * COLLABORATIVE_WEIGHT, Double::sum);
            }
        }

        // ── Couche 2 : Content-based (catégories favorites) ──────────────
        List<Object[]> topCategories = interactionRepo.findTopCategoriesByCustomer(customerId);
        if (!topCategories.isEmpty()) {
            // Récupérer les IDs des catégories favorites (top 5)
            List<Long> favCategoryIds = topCategories.stream()
                    .limit(5)
                    .map(row -> ((Number) row[0]).longValue())
                    .toList();

            List<Product> categoryProducts = productRepository.findAllAvailablePremiumFirst().stream()
                    .filter(p -> p.getCategory() != null
                            && favCategoryIds.contains(p.getCategory().getId())
                            && !excludeSet.contains(p.getId()))
                    .limit(50)
                    .toList();

            // Calculer le poids de la catégorie
            Map<Long, Double> categoryWeights = new HashMap<>();
            double maxCatWeight = topCategories.isEmpty() ? 1 :
                    ((Number) topCategories.get(0)[1]).doubleValue();
            for (Object[] row : topCategories) {
                Long catId = ((Number) row[0]).longValue();
                double weight = ((Number) row[1]).doubleValue() / maxCatWeight;
                categoryWeights.put(catId, weight);
            }

            for (Product p : categoryProducts) {
                double catWeight = categoryWeights.getOrDefault(p.getCategory().getId(), 0.5);
                double contentScore = catWeight * CONTENT_WEIGHT;

                // Bonus pour les produits en promo
                if (p.isOnPromotion()) contentScore *= 1.3;

                // Bonus pour les produits en tendance
                if (p.isCurrentlyTrending()) contentScore *= 1.2;

                scores.merge(p.getId(), contentScore, Double::sum);
            }
        }

        // ── Couche 3 : Contextuel (pays, popularité locale) ──────────────
        if (countryCode != null && !countryCode.isBlank()) {
            Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
            List<Object[]> popularLocal = interactionRepo.findPopularByCountry(countryCode, since, 30);
            double maxPop = popularLocal.isEmpty() ? 1 : ((Number) popularLocal.get(0)[1]).doubleValue();

            for (Object[] row : popularLocal) {
                Long pId = ((Number) row[0]).longValue();
                if (excludeSet.contains(pId)) continue;
                double popScore = ((Number) row[1]).doubleValue() / maxPop;
                scores.merge(pId, popScore * CONTEXTUAL_WEIGHT, Double::sum);
            }
        }

        return resolveAndFilter(scores, limit);
    }

    /**
     * Cold Start : recommandations pour les nouveaux clients sans historique.
     * Combine produits populaires du pays + promotions + tendances.
     */
    private List<Product> getColdStartRecommendations(String countryCode, int limit) {
        Map<Long, Double> scores = new LinkedHashMap<>();

        // Produits populaires du pays (derniers 30 jours)
        if (countryCode != null && !countryCode.isBlank()) {
            Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
            List<Object[]> popular = interactionRepo.findPopularByCountry(countryCode, since, limit * 3);
            double maxPop = popular.isEmpty() ? 1 : ((Number) popular.get(0)[1]).doubleValue();
            for (Object[] row : popular) {
                Long pId = ((Number) row[0]).longValue();
                scores.merge(pId, ((Number) row[1]).doubleValue() / maxPop, Double::sum);
            }
        }

        // Si pas assez de résultats, compléter avec les produits en tendance + featured
        if (scores.size() < limit) {
            List<Product> trending = productRepository.findCurrentlyTrendingAvailable();
            for (Product p : trending) {
                scores.putIfAbsent(p.getId(), 0.8);
            }
        }

        if (scores.size() < limit) {
            List<Product> featured = productRepository.findFeaturedForHomepage();
            for (Product p : featured) {
                scores.putIfAbsent(p.getId(), 0.5);
            }
        }

        return resolveAndFilter(scores, limit);
    }

    /**
     * Fallback absolu : si tout échoue, retourne les produits populaires récents.
     */
    private List<Product> getFallbackRecommendations(int limit) {
        return productRepository.findPopularForHomepage().stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Ajoute des scores content-based basés sur la similarité avec un produit source.
     */
    private void addContentBasedScores(Map<Long, Double> scores, Product source) {
        List<Product> candidates = productRepository.findAllAvailablePremiumFirst().stream()
                .filter(p -> !p.getId().equals(source.getId()))
                .limit(100)
                .toList();

        for (Product candidate : candidates) {
            double similarity = calculateSimilarity(source, candidate);
            if (similarity > 0.1) {
                scores.merge(candidate.getId(), similarity * CONTENT_WEIGHT, Double::sum);
            }
        }
    }

    /**
     * Calcule la similarité entre deux produits (0.0 - 1.0).
     * Facteurs : catégorie, gamme de prix, même vendeur, promotions.
     */
    private double calculateSimilarity(Product a, Product b) {
        double sim = 0.0;

        // Même catégorie = forte similarité
        if (a.getCategory() != null && b.getCategory() != null
                && a.getCategory().getId().equals(b.getCategory().getId())) {
            sim += 0.5;
        }

        // Gamme de prix similaire (±30%)
        long priceA = a.getEffectivePriceCfa();
        long priceB = b.getEffectivePriceCfa();
        if (priceA > 0 && priceB > 0) {
            double ratio = (double) Math.min(priceA, priceB) / Math.max(priceA, priceB);
            if (ratio > 0.7) {
                sim += 0.25 * ratio;
            }
        }

        // Même vendeur = léger bonus
        if (a.getVendor() != null && b.getVendor() != null
                && a.getVendor().getId().equals(b.getVendor().getId())) {
            sim += 0.15;
        }

        // Bonus si le candidat est en promo
        if (b.isOnPromotion()) sim += 0.1;

        return Math.min(1.0, sim);
    }

    /**
     * Résout les IDs en Product, filtre les indisponibles, trie par score.
     */
    private List<Product> resolveAndFilter(Map<Long, Double> scores, int limit) {
        if (scores.isEmpty()) return List.of();

        // Trier par score décroissant
        List<Long> sortedIds = scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(limit * 2) // marge pour le filtrage
                .toList();

        // Charger les produits
        List<Product> products = new ArrayList<>();
        for (Long id : sortedIds) {
            if (products.size() >= limit) break;
            productRepository.findByIdWithDetails(id).ifPresent(p -> {
                if (p.isAvailable()
                        && (p.getVendor() == null
                            || (p.getVendor().isActive()
                                && p.getVendor().getVendorStatus() == VendorStatus.ACTIVE))) {
                    products.add(p);
                }
            });
        }

        // Diversification : pas plus de 3 produits du même vendeur
        return diversify(products, limit, 3);
    }

    /**
     * Diversifie les recommandations : max N produits du même vendeur.
     */
    private List<Product> diversify(List<Product> products, int limit, int maxPerVendor) {
        Map<Long, Integer> vendorCount = new HashMap<>();
        List<Product> result = new ArrayList<>();

        for (Product p : products) {
            if (result.size() >= limit) break;
            Long vendorId = (p.getVendor() != null) ? p.getVendor().getId() : -1L;
            int count = vendorCount.getOrDefault(vendorId, 0);
            if (count < maxPerVendor) {
                result.add(p);
                vendorCount.put(vendorId, count + 1);
            }
        }
        return result;
    }
}
