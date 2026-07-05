package com.bolas.ecommerce.service;

import com.bolas.ecommerce.dto.FraudAlertDto;
import com.bolas.ecommerce.model.*;
import com.bolas.ecommerce.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Calcule le TrustScore (0-100) de chaque vendeur basé sur des signaux objectifs.
 *
 * Détecte automatiquement les anomalies (fraude potentielle) et flagge
 * les vendeurs suspects pour examen admin.
 *
 * Ce système répond au vrai problème des marketplaces africaines :
 * les vendeurs fantômes et les arnaques.
 */
@Service
public class VendorTrustScoreService {

    private static final Logger log = LoggerFactory.getLogger(VendorTrustScoreService.class);
    private static final ZoneId ZONE = ZoneId.of("Africa/Abidjan");

    // Seuils de détection de fraude
    private static final double MAX_CANCELLATION_RATE = 0.50;   // 50% d'annulations
    private static final int MAX_REPORTS_30_DAYS = 3;            // 3 signalements en 30 jours
    private static final int SCORE_DROP_THRESHOLD = 25;          // Chute de 25+ points
    private static final int SPAM_PRODUCT_THRESHOLD = 50;        // 50+ produits en < 7 jours
    private static final int FLAG_SCORE_THRESHOLD = 30;          // Score < 30 → alerte

    private final VendorTrustScoreRepository trustScoreRepository;
    private final VendorUserRepository vendorUserRepository;
    private final CustomerOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final ReportRepository reportRepository;

    public VendorTrustScoreService(VendorTrustScoreRepository trustScoreRepository,
                                    VendorUserRepository vendorUserRepository,
                                    CustomerOrderRepository orderRepository,
                                    ProductRepository productRepository,
                                    ReviewRepository reviewRepository,
                                    ReportRepository reportRepository) {
        this.trustScoreRepository = trustScoreRepository;
        this.vendorUserRepository = vendorUserRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
        this.reportRepository = reportRepository;
    }

    /**
     * Calcule ou recalcule le TrustScore d'un vendeur.
     */
    @Transactional
    public VendorTrustScore calculateScore(VendorUser vendor) {
        VendorTrustScore ts = trustScoreRepository.findByVendorId(vendor.getId())
                .orElseGet(() -> {
                    VendorTrustScore newTs = new VendorTrustScore(vendor);
                    return newTs;
                });

        int previousScore = ts.getScore();

        // ── Signal 1 : Identité vérifiée (max 20 pts) ────────────────────
        ts.setIdentityPoints(calculateIdentityPoints(vendor));

        // ── Signal 2 : Taux de livraison (max 25 pts) ────────────────────
        ts.setDeliveryPoints(calculateDeliveryPoints(vendor));

        // ── Signal 3 : Ancienneté (max 10 pts) ──────────────────────────
        ts.setSeniorityPoints(calculateSeniorityPoints(vendor));

        // ── Signal 4 : Note moyenne avis (max 15 pts) ───────────────────
        ts.setReviewPoints(calculateReviewPoints(vendor));

        // ── Signal 5 : Rapidité de réponse (max 10 pts) ─────────────────
        ts.setResponsePoints(calculateResponsePoints(vendor));

        // ── Signal 6 : Complétude profil (max 10 pts) ───────────────────
        ts.setProfilePoints(calculateProfilePoints(vendor));

        // ── Signal 7 : Volume de commandes (max 10 pts) ─────────────────
        ts.setVolumePoints(calculateVolumePoints(vendor));

        // ── Signal 8 : Malus signalements (0 à -10) ────────────────────
        ts.setReportPenalty(calculateReportPenalty(vendor));

        // Recalcul total
        ts.recalculateTotal();

        // ── Détection d'anomalies / Fraude ──────────────────────────────
        detectFraud(ts, vendor, previousScore);

        trustScoreRepository.save(ts);
        log.debug("TrustScore vendeur {} ({}): {} → {} [{}]",
                vendor.getId(), vendor.getDisplayName(),
                previousScore, ts.getScore(), ts.getTrustLevel());

        return ts;
    }

    /**
     * Récupère le TrustScore d'un vendeur (calculé ou depuis le cache).
     */
    @Transactional(readOnly = true)
    public Optional<VendorTrustScore> getScore(Long vendorId) {
        return trustScoreRepository.findByVendorId(vendorId);
    }

    /**
     * Recalcule les scores de tous les vendeurs actifs.
     */
    @Transactional
    public int recalculateAll() {
        List<VendorUser> activeVendors = vendorUserRepository.findByVendorStatus(VendorStatus.ACTIVE);
        int count = 0;
        for (VendorUser vendor : activeVendors) {
            try {
                calculateScore(vendor);
                count++;
            } catch (Exception e) {
                log.error("Erreur calcul TrustScore vendeur {}", vendor.getId(), e);
            }
        }
        log.info("TrustScore recalculé pour {} vendeurs actifs", count);
        return count;
    }

    /**
     * Liste des alertes fraude pour le dashboard admin.
     */
    @Transactional(readOnly = true)
    public List<FraudAlertDto> getFraudAlerts() {
        List<VendorTrustScore> flagged = trustScoreRepository.findFlaggedForReview();
        List<FraudAlertDto> alerts = new ArrayList<>();

        for (VendorTrustScore ts : flagged) {
            VendorUser v = ts.getVendor();
            FraudAlertDto alert = new FraudAlertDto(
                    v.getId(), v.getUsername(), v.getDisplayName(),
                    ts.getScore(), ts.getTrustLevel(), ts.getFlagReason());
            alert.setEmail(v.getEmail());
            alert.setPhone(v.getPhone());
            alert.setProductCount((int) productRepository.countByVendor(v));

            // Calcul du taux d'annulation
            long delivered = orderRepository.countByVendorAndStatus(v, OrderStatus.DELIVERED);
            long cancelled = orderRepository.countByVendorAndStatus(v, OrderStatus.CANCELLED);
            long total = delivered + cancelled;
            alert.setCancellationRate(total > 0 ? (double) cancelled / total : 0);

            // Ancienneté
            if (v.getSubscriptionStartsAt() != null) {
                long days = ChronoUnit.DAYS.between(
                        v.getSubscriptionStartsAt().atZone(ZONE).toInstant(), Instant.now());
                alert.setAccountAgeDays(days);
            }

            alerts.add(alert);
        }

        return alerts;
    }

    /**
     * Résout un flag fraude (admin a examiné le vendeur).
     */
    @Transactional
    public void resolveFlag(Long vendorId) {
        trustScoreRepository.findByVendorId(vendorId).ifPresent(ts -> {
            ts.setFlaggedForReview(false);
            ts.setFlagReason(null);
            trustScoreRepository.save(ts);
            log.info("Flag fraude résolu pour vendeur {}", vendorId);
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CALCUL DES SIGNAUX
    // ═══════════════════════════════════════════════════════════════════════

    /** Signal 1 : Identité vérifiée — max 20 pts */
    private int calculateIdentityPoints(VendorUser vendor) {
        if (Boolean.TRUE.equals(vendor.getIdDocVerified())) return 20;
        if (vendor.getIdDocumentUrl() != null && !vendor.getIdDocumentUrl().isBlank()) return 8;
        return 0;
    }

    /** Signal 2 : Taux de livraison — max 25 pts */
    private int calculateDeliveryPoints(VendorUser vendor) {
        long delivered = orderRepository.countByVendorAndStatus(vendor, OrderStatus.DELIVERED);
        long cancelled = orderRepository.countByVendorAndStatus(vendor, OrderStatus.CANCELLED);
        long total = delivered + cancelled;

        if (total == 0) return 5; // Pas encore de commandes → score neutre

        double deliveryRate = (double) delivered / total;
        return (int) Math.round(deliveryRate * 25);
    }

    /** Signal 3 : Ancienneté — max 10 pts (6 mois+ = max) */
    private int calculateSeniorityPoints(VendorUser vendor) {
        if (vendor.getSubscriptionStartsAt() == null) return 1;

        long days = ChronoUnit.DAYS.between(
                vendor.getSubscriptionStartsAt().atZone(ZONE).toInstant(), Instant.now());

        if (days >= 180) return 10; // 6 mois+
        if (days >= 90) return 7;   // 3 mois
        if (days >= 30) return 4;   // 1 mois
        return 2;
    }

    /** Signal 4 : Note moyenne avis — max 15 pts */
    private int calculateReviewPoints(VendorUser vendor) {
        try {
            double avgRating = reviewRepository.averageRatingByVendor(vendor);
            long reviewCount = reviewRepository.countApprovedByVendor(vendor);

            if (reviewCount == 0) return 3; // Pas d'avis → neutre

            // Score = (rating / 5) * 12, avec bonus si beaucoup d'avis
            double base = (avgRating / 5.0) * 12;
            double volumeBonus = Math.min(3, reviewCount * 0.3); // max +3 pts
            return (int) Math.round(base + volumeBonus);
        } catch (Exception e) {
            return 3;
        }
    }

    /** Signal 5 : Rapidité de réponse aux commandes — max 10 pts */
    private int calculateResponsePoints(VendorUser vendor) {
        // Basé sur le nombre de commandes confirmées rapidement
        long pending = orderRepository.countByVendorAndStatus(vendor, OrderStatus.PENDING);
        long confirmed = orderRepository.countByVendorAndStatus(vendor, OrderStatus.CONFIRMED);
        long total = orderRepository.countByVendor(vendor);

        if (total == 0) return 5; // Pas de commandes → neutre

        // Moins de commandes en attente = meilleur score
        double pendingRate = (double) pending / Math.max(1, total);
        if (pendingRate < 0.1) return 10; // <10% en attente → excellent
        if (pendingRate < 0.2) return 7;
        if (pendingRate < 0.4) return 4;
        return 2;
    }

    /** Signal 6 : Complétude du profil boutique — max 10 pts */
    private int calculateProfilePoints(VendorUser vendor) {
        int points = 0;

        if (vendor.getShopName() != null && !vendor.getShopName().isBlank()) points += 1;
        if (vendor.getShopDescription() != null && !vendor.getShopDescription().isBlank()) points += 1;
        if (vendor.getLogoUrl() != null && !vendor.getLogoUrl().isBlank()) points += 2;
        if (vendor.getShopLatitude() != null || (vendor.getShopAddress() != null && !vendor.getShopAddress().isBlank())) points += 2;
        if (vendor.hasShopInfos()) points += 2;
        if (vendor.getShopHours() != null && !vendor.getShopHours().isBlank()) points += 1;
        if (vendor.getBannerUrl() != null && !vendor.getBannerUrl().isBlank()) points += 1;

        return Math.min(10, points);
    }

    /** Signal 7 : Volume de commandes traitées — max 10 pts */
    private int calculateVolumePoints(VendorUser vendor) {
        long delivered = orderRepository.countByVendorAndStatus(vendor, OrderStatus.DELIVERED);

        if (delivered >= 100) return 10;
        if (delivered >= 50) return 8;
        if (delivered >= 20) return 6;
        if (delivered >= 10) return 4;
        if (delivered >= 3) return 2;
        return 0;
    }

    /** Signal 8 : Malus signalements — 0 à -10 */
    private int calculateReportPenalty(VendorUser vendor) {
        try {
            long reportCount = reportRepository.countByVendorId(vendor.getId());
            if (reportCount == 0) return 0;
            if (reportCount == 1) return -2;
            if (reportCount == 2) return -5;
            return -10; // 3+ signalements
        } catch (Exception e) {
            return 0;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DÉTECTION DE FRAUDE
    // ═══════════════════════════════════════════════════════════════════════

    private void detectFraud(VendorTrustScore ts, VendorUser vendor, int previousScore) {
        List<String> reasons = new ArrayList<>();

        // 1. Taux d'annulation trop élevé
        long delivered = orderRepository.countByVendorAndStatus(vendor, OrderStatus.DELIVERED);
        long cancelled = orderRepository.countByVendorAndStatus(vendor, OrderStatus.CANCELLED);
        long total = delivered + cancelled;
        if (total >= 5) {
            double cancelRate = (double) cancelled / total;
            if (cancelRate > MAX_CANCELLATION_RATE) {
                reasons.add(String.format("Taux d'annulation %.0f%% sur %d commandes", cancelRate * 100, total));
            }
        }

        // 2. Trop de signalements récents
        try {
            long reportCount = reportRepository.countByVendorId(vendor.getId());
            if (reportCount >= MAX_REPORTS_30_DAYS) {
                reasons.add(String.format("%d signalements reçus", reportCount));
            }
        } catch (Exception e) {
            // pas critique
        }

        // 3. Chute brutale du score
        if (previousScore > 50 && ts.getScore() < (previousScore - SCORE_DROP_THRESHOLD)) {
            reasons.add(String.format("Score en chute: %d → %d", previousScore, ts.getScore()));
        }

        // 4. Suspicion de spam (trop de produits pour un compte récent)
        if (vendor.getSubscriptionStartsAt() != null) {
            long accountDays = ChronoUnit.DAYS.between(
                    vendor.getSubscriptionStartsAt().atZone(ZONE).toInstant(), Instant.now());
            if (accountDays < 7) {
                long productCount = productRepository.countByVendor(vendor);
                if (productCount > SPAM_PRODUCT_THRESHOLD) {
                    reasons.add(String.format("%d produits en %d jours (suspicion de spam)", productCount, accountDays));
                }
            }
        }

        // 5. Score très bas
        if (ts.getScore() < FLAG_SCORE_THRESHOLD && total >= 3) {
            reasons.add(String.format("Score de confiance très bas: %d/100", ts.getScore()));
        }

        // Appliquer le flag
        if (!reasons.isEmpty()) {
            ts.setFlaggedForReview(true);
            ts.setFlagReason(String.join(" | ", reasons));
            log.warn("🚨 Vendeur {} flaggé pour fraude: {}", vendor.getId(), ts.getFlagReason());
        }
    }
}
