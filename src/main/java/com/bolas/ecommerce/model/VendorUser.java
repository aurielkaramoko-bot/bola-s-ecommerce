package com.bolas.ecommerce.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "vendor_users")
public class VendorUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Size(max = 100)
    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @NotBlank @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String passwordHash;

    @NotBlank @Size(max = 40)
    @Column(nullable = false, length = 40)
    private String phone;

    /** Nom affiché de la boutique */
    @Size(max = 150)
    @Column(length = 150)
    private String shopName;

    /** Courte description de l'activité */
    @Size(max = 500)
    @Column(length = 500)
    private String shopDescription;

    /** Email de contact du vendeur */
    @Size(max = 200)
    @Column(length = 200)
    private String email;

    /** URL du logo / photo de boutique */
    @Size(max = 2000)
    @Column(name = "logo_url", length = 2000)
    private String logoUrl;

    /** URL de la pièce d'identité (pour vérification admin) */
    @Size(max = 2000)
    @Column(name = "id_document_url", length = 2000)
    private String idDocumentUrl;

    /** Niche/catégorie demandée si elle n'existe pas dans la liste */
    @Size(max = 300)
    @Column(name = "requested_niche", length = 300)
    private String requestedNiche;

    /** Plan d'abonnement : GRATUIT (max 10 produits) ou PRO */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VendorPlan plan = VendorPlan.GRATUIT;

    /**
     * Statut du compte :
     *  PENDING   → demande soumise via le formulaire public, en attente admin
     *  ACTIVE    → vendeur approuvé
     *  SUSPENDED → suspendu par l'admin
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VendorStatus vendorStatus = VendorStatus.PENDING;

    /** Alias maintenu pour compatibilité avec le code existant */
    @Column(nullable = false)
    private boolean active = false;

    /**
     * Résultat du scan Vision API sur la pièce d'identité :
     *  null      → pas encore analysé
     *  true      → document d'identité détecté
     *  false     → document non reconnu (photo aléatoire)
     */
    @Column(name = "id_doc_verified")
    private Boolean idDocVerified;

    /** Raison de la suspension (visible dans le message d'erreur au vendeur) */
    @Column(name = "suspension_reason", length = 500)
    private String suspensionReason;

    /**
     * Type de suspension :
     *  true  → douce : vendeur bloqué mais produits restent visibles
     *  false → totale : produits masqués automatiquement
     */
    @Column(name = "soft_suspend")
    private boolean softSuspend = true;

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getShopDescription() { return shopDescription; }
    public void setShopDescription(String shopDescription) { this.shopDescription = shopDescription; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getIdDocumentUrl() { return idDocumentUrl; }
    public void setIdDocumentUrl(String idDocumentUrl) { this.idDocumentUrl = idDocumentUrl; }

    public String getRequestedNiche() { return requestedNiche; }
    public void setRequestedNiche(String requestedNiche) { this.requestedNiche = requestedNiche; }

    public VendorPlan getPlan() { return plan; }
    public void setPlan(VendorPlan plan) { this.plan = plan; }

    public VendorStatus getVendorStatus() { return vendorStatus; }
    public void setVendorStatus(VendorStatus vendorStatus) { this.vendorStatus = vendorStatus; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Boolean getIdDocVerified() { return idDocVerified; }
    public void setIdDocVerified(Boolean idDocVerified) { this.idDocVerified = idDocVerified; }

    public String getSuspensionReason() { return suspensionReason; }
    public void setSuspensionReason(String suspensionReason) { this.suspensionReason = suspensionReason; }

    public boolean isSoftSuspend() { return softSuspend; }
    public void setSoftSuspend(boolean softSuspend) { this.softSuspend = softSuspend; }

    /** Nom d'affichage : priorité shopName, sinon username */
    public String getDisplayName() {
        return (shopName != null && !shopName.isBlank()) ? shopName : username;
    }
}
