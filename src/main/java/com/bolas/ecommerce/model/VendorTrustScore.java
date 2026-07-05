package com.bolas.ecommerce.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Score de confiance calculé pour un vendeur (0-100).
 * Mis à jour périodiquement par le TrustScoreScheduler.
 *
 * Chaque signal contribue indépendamment au score total :
 *  - identityPoints  (max 20) : pièce d'identité vérifiée
 *  - deliveryPoints   (max 25) : taux livraisons vs annulations
 *  - seniorityPoints  (max 10) : ancienneté du compte
 *  - reviewPoints     (max 15) : note moyenne des avis
 *  - responsePoints   (max 10) : rapidité de traitement des commandes
 *  - profilePoints    (max 10) : complétude du profil boutique
 *  - volumePoints     (max 10) : nb de commandes traitées avec succès
 *  - reportPenalty    (max -10) : malus pour signalements
 */
@Entity
@Table(name = "vendor_trust_scores")
public class VendorTrustScore {

    @Id
    @Column(name = "vendor_id")
    private Long vendorId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "vendor_id")
    private VendorUser vendor;

    /** Score global 0-100 */
    @Column(nullable = false)
    private int score = 0;

    // ─── Détail par signal ─────────────────────────────────────────────────

    @Column(name = "identity_points", nullable = false)
    private int identityPoints = 0;

    @Column(name = "delivery_points", nullable = false)
    private int deliveryPoints = 0;

    @Column(name = "seniority_points", nullable = false)
    private int seniorityPoints = 0;

    @Column(name = "review_points", nullable = false)
    private int reviewPoints = 0;

    @Column(name = "response_points", nullable = false)
    private int responsePoints = 0;

    @Column(name = "profile_points", nullable = false)
    private int profilePoints = 0;

    @Column(name = "volume_points", nullable = false)
    private int volumePoints = 0;

    /** Malus (valeur négative ou 0) pour les signalements */
    @Column(name = "report_penalty", nullable = false)
    private int reportPenalty = 0;

    // ─── Métadonnées ───────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "trust_level", nullable = false, length = 20)
    private TrustLevel trustLevel = TrustLevel.NEW;

    @Column(name = "last_calculated_at")
    private Instant lastCalculatedAt;

    /** Vendeur flaggé automatiquement pour examen admin */
    @Column(name = "flagged_for_review", nullable = false)
    private boolean flaggedForReview = false;

    /** Raison du flag (ex: "Taux d'annulation 67% sur 10 dernières commandes") */
    @Column(name = "flag_reason", length = 500)
    private String flagReason;

    // ─── Constructeurs ─────────────────────────────────────────────────────

    public VendorTrustScore() {}

    public VendorTrustScore(VendorUser vendor) {
        this.vendor = vendor;
        this.vendorId = vendor.getId();
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    /** Recalcule le score total à partir des composantes */
    public void recalculateTotal() {
        this.score = Math.max(0, Math.min(100,
                identityPoints + deliveryPoints + seniorityPoints +
                reviewPoints + responsePoints + profilePoints +
                volumePoints + reportPenalty));
        this.trustLevel = TrustLevel.fromScore(this.score);
        this.lastCalculatedAt = Instant.now();
    }

    /** Badge HTML pour Thymeleaf */
    public String getTrustBadgeText() {
        return trustLevel.getBadgeText();
    }

    // ─── Getters / Setters ─────────────────────────────────────────────────

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public VendorUser getVendor() { return vendor; }
    public void setVendor(VendorUser vendor) { this.vendor = vendor; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getIdentityPoints() { return identityPoints; }
    public void setIdentityPoints(int identityPoints) { this.identityPoints = identityPoints; }

    public int getDeliveryPoints() { return deliveryPoints; }
    public void setDeliveryPoints(int deliveryPoints) { this.deliveryPoints = deliveryPoints; }

    public int getSeniorityPoints() { return seniorityPoints; }
    public void setSeniorityPoints(int seniorityPoints) { this.seniorityPoints = seniorityPoints; }

    public int getReviewPoints() { return reviewPoints; }
    public void setReviewPoints(int reviewPoints) { this.reviewPoints = reviewPoints; }

    public int getResponsePoints() { return responsePoints; }
    public void setResponsePoints(int responsePoints) { this.responsePoints = responsePoints; }

    public int getProfilePoints() { return profilePoints; }
    public void setProfilePoints(int profilePoints) { this.profilePoints = profilePoints; }

    public int getVolumePoints() { return volumePoints; }
    public void setVolumePoints(int volumePoints) { this.volumePoints = volumePoints; }

    public int getReportPenalty() { return reportPenalty; }
    public void setReportPenalty(int reportPenalty) { this.reportPenalty = reportPenalty; }

    public TrustLevel getTrustLevel() { return trustLevel; }
    public void setTrustLevel(TrustLevel trustLevel) { this.trustLevel = trustLevel; }

    public Instant getLastCalculatedAt() { return lastCalculatedAt; }
    public void setLastCalculatedAt(Instant lastCalculatedAt) { this.lastCalculatedAt = lastCalculatedAt; }

    public boolean isFlaggedForReview() { return flaggedForReview; }
    public void setFlaggedForReview(boolean flaggedForReview) { this.flaggedForReview = flaggedForReview; }

    public String getFlagReason() { return flagReason; }
    public void setFlagReason(String flagReason) { this.flagReason = flagReason; }
}
