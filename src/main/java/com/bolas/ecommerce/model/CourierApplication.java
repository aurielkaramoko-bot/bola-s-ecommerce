package com.bolas.ecommerce.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Demande de livreur soumise par un vendeur.
 * L'admin approuve ou rejette depuis son panel.
 */
@Entity
@Table(name = "courier_applications")
public class CourierApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom complet du livreur proposé */
    @Column(nullable = false, length = 150)
    private String courierName;

    /** Téléphone WhatsApp du livreur */
    @Column(nullable = false, length = 40)
    private String courierPhone;

    /** Zone / quartier de livraison */
    @Column(length = 200)
    private String zone;

    /** Photo du livreur (selfie ou photo d'identité) */
    @Column(name = "photo_url", length = 2000)
    private String photoUrl;

    /** Pièce d'identité du livreur (CNI, passeport, permis) */
    @Column(name = "id_document_url", length = 2000)
    private String idDocumentUrl;

    /** Résultat scan Vision API sur la pièce d'identité */
    @Column(name = "id_doc_verified")
    private Boolean idDocVerified;

    /** Vendeur qui a soumis la demande */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private VendorUser vendor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CourierApplicationStatus status = CourierApplicationStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    /** Raison de la suspension (visible par l'admin) */
    @Column(name = "suspension_reason", length = 500)
    private String suspensionReason;

    /** Date de la dernière action (approbation, suspension, etc) */
    @Column(name = "last_action_at")
    private LocalDateTime lastActionAt;

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public String getCourierName() { return courierName; }
    public void setCourierName(String courierName) { this.courierName = courierName; }

    public String getCourierPhone() { return courierPhone; }
    public void setCourierPhone(String courierPhone) { this.courierPhone = courierPhone; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getIdDocumentUrl() { return idDocumentUrl; }
    public void setIdDocumentUrl(String idDocumentUrl) { this.idDocumentUrl = idDocumentUrl; }

    public Boolean getIdDocVerified() { return idDocVerified; }
    public void setIdDocVerified(Boolean idDocVerified) { this.idDocVerified = idDocVerified; }

    public VendorUser getVendor() { return vendor; }
    public void setVendor(VendorUser vendor) { this.vendor = vendor; }

    public CourierApplicationStatus getStatus() { return status; }
    public void setStatus(CourierApplicationStatus status) { this.status = status; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }

    public String getSuspensionReason() { return suspensionReason; }
    public void setSuspensionReason(String suspensionReason) { this.suspensionReason = suspensionReason; }

    public LocalDateTime getLastActionAt() { return lastActionAt; }
    public void setLastActionAt(LocalDateTime lastActionAt) { this.lastActionAt = lastActionAt; }
}
