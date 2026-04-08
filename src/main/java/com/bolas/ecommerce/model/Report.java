package com.bolas.ecommerce.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.Instant;

/**
 * Signalement d'un produit ou d'un vendeur par un acheteur.
 */
@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Type de cible : "PRODUCT" ou "VENDOR" */
    @NotBlank @Size(max = 20)
    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    /** ID de la cible (produit ou vendeur) */
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /** Nom de la cible (pour affichage) */
    @Size(max = 200)
    @Column(name = "target_name", length = 200)
    private String targetName;

    /** Email du signaleur */
    @Size(max = 200)
    @Column(name = "reporter_email", length = 200)
    private String reporterEmail;

    /** Raison du signalement */
    @NotBlank @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    /** Traité par l'admin */
    @Column(nullable = false)
    private boolean resolved = false;

    // ─── Getters / Setters ────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public String getReporterEmail() { return reporterEmail; }
    public void setReporterEmail(String reporterEmail) { this.reporterEmail = reporterEmail; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
}
