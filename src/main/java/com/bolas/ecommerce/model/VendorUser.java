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
    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, unique = true, length = 200)
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
    @Column(name = "soft_suspend", nullable = true)
    private Boolean softSuspend = true;

    /** URL bannière publicitaire (PREMIUM uniquement) — affichée sur la homepage */
    @Size(max = 2000)
    @Column(name = "banner_url", length = 2000)
    private String bannerUrl;

    /** Date de début de l'abonnement */
    @Column(name = "subscription_starts_at")
    private java.time.LocalDate subscriptionStartsAt;

    /** Date d'expiration de l'abonnement (null = pas d'abonnement actif) */
    @Column(name = "subscription_expires_at")
    private java.time.LocalDate subscriptionExpiresAt;



    /** Latitude GPS de la boutique physique */
    @Column(name = "shop_latitude")
    private Double shopLatitude;

    /** Longitude GPS de la boutique physique */
    @Column(name = "shop_longitude")
    private Double shopLongitude;

    /** Adresse textuelle de la boutique */
    @jakarta.validation.constraints.Size(max = 500)
    @Column(name = "shop_address", length = 500)
    private String shopAddress;

    /** Réduction globale sur toute la boutique (0 ou null = pas de réduction) */
    @Column(name = "shop_discount_percent")
    private Integer shopDiscountPercent;

    /** Date de fin de la réduction boutique (null = pas de date de fin) */
    @Column(name = "shop_discount_ends_at")
    private java.time.LocalDate shopDiscountEndsAt;

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

    public boolean isSoftSuspend() { return softSuspend != null ? softSuspend : true; }
    public void setSoftSuspend(boolean softSuspend) { this.softSuspend = softSuspend; }

    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }

    public java.time.LocalDate getSubscriptionStartsAt() { return subscriptionStartsAt; }
    public void setSubscriptionStartsAt(java.time.LocalDate subscriptionStartsAt) { this.subscriptionStartsAt = subscriptionStartsAt; }

    public java.time.LocalDate getSubscriptionExpiresAt() { return subscriptionExpiresAt; }
    public void setSubscriptionExpiresAt(java.time.LocalDate subscriptionExpiresAt) { this.subscriptionExpiresAt = subscriptionExpiresAt; }



    public Double getShopLatitude() { return shopLatitude; }
    public void setShopLatitude(Double shopLatitude) { this.shopLatitude = shopLatitude; }

    public Double getShopLongitude() { return shopLongitude; }
    public void setShopLongitude(Double shopLongitude) { this.shopLongitude = shopLongitude; }

    public String getShopAddress() { return shopAddress; }
    public void setShopAddress(String shopAddress) { this.shopAddress = shopAddress; }

    /** La boutique a une localisation GPS renseignée */
    public boolean hasLocation() {
        return shopLatitude != null && shopLongitude != null;
    }

    /** Jours restants avant expiration (négatif = expiré) */
    public long getDaysUntilExpiry() {
        if (subscriptionExpiresAt == null) return Long.MAX_VALUE;
        return java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), subscriptionExpiresAt);
    }

    /** Nom d'affichage : priorité shopName, sinon username */
    public String getDisplayName() {
        return (shopName != null && !shopName.isBlank()) ? shopName : username;
    }

    // ─── Plan-based helpers ──────────────────────────────────────────────────

    private boolean isPaidPlan() {
        return plan == VendorPlan.PRO_LOCAL || plan == VendorPlan.PRO || plan == VendorPlan.PREMIUM;
    }

    /** Le vendeur peut gérer ses commandes lui-même (confirmer, préparer, expédier) — TOUS les vendeurs */
    public boolean canManageOrders() { return true; }

    /** Le vendeur peut chatter avec ses clients */
    public boolean canChat() { return isPaidPlan(); }

    /** Le vendeur a accès aux statistiques basiques */
    public boolean canViewStats() { return isPaidPlan(); }

    /** Le vendeur a accès aux statistiques avancées (graphiques 6 mois, conversion) */
    public boolean hasAdvancedStats() { return plan == VendorPlan.PREMIUM; }

    /** Badge vérifié ⭐ visible partout */
    public boolean isVerified() { return plan == VendorPlan.PREMIUM; }

    /** Le vendeur peut répondre aux avis clients */
    public boolean canRespondToReviews() { return plan == VendorPlan.PREMIUM; }

    /** Le vendeur bénéficie d'une mise en avant prioritaire homepage et recherche */
    public boolean hasHomepagePriority() { return plan == VendorPlan.PREMIUM; }

    /** Nombre maximum de produits autorisés */
    public int getMaxProducts() { return plan == VendorPlan.GRATUIT ? 10 : Integer.MAX_VALUE; }

    /** Nombre maximum de sous-vendeurs autorisés par plan */
    public int getMaxSellers() {
        return switch (plan) {
            case GRATUIT -> 0;
            case PRO_LOCAL, PRO -> 2;
            case PREMIUM -> 5;
        };
    }

    /** Le vendeur peut gérer des sous-vendeurs */
    public boolean canManageSellers() { return isPaidPlan(); }

    // ─── Réduction boutique ─────────────────────────────────────────────────

    public Integer getShopDiscountPercent() { return shopDiscountPercent; }
    public void setShopDiscountPercent(Integer shopDiscountPercent) { this.shopDiscountPercent = shopDiscountPercent; }

    public java.time.LocalDate getShopDiscountEndsAt() { return shopDiscountEndsAt; }
    public void setShopDiscountEndsAt(java.time.LocalDate shopDiscountEndsAt) { this.shopDiscountEndsAt = shopDiscountEndsAt; }

    /** La réduction boutique est-elle active ? (% > 0 et date non dépassée) */
    public boolean isShopDiscountActive() {
        if (shopDiscountPercent == null || shopDiscountPercent <= 0) return false;
        if (shopDiscountEndsAt != null && shopDiscountEndsAt.isBefore(java.time.LocalDate.now())) return false;
        return true;
    }
}
