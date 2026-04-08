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

    public VendorPlan getPlan() { return plan; }
    public void setPlan(VendorPlan plan) { this.plan = plan; }

    public VendorStatus getVendorStatus() { return vendorStatus; }
    public void setVendorStatus(VendorStatus vendorStatus) { this.vendorStatus = vendorStatus; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /** Nom d'affichage : priorité shopName, sinon username */
    public String getDisplayName() {
        return (shopName != null && !shopName.isBlank()) ? shopName : username;
    }
}
