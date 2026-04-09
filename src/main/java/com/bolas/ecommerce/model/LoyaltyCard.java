package com.bolas.ecommerce.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * Carte de fidélité créée par un vendeur pour un client fidèle.
 * Le client utilise le code lors de sa commande WhatsApp ou en boutique.
 *
 * Restrictions par plan :
 *  - GRATUIT    : peut créer des cartes (fonctionnalité de base)
 *  - PRO/PRO_LOCAL/PREMIUM : idem, sans restriction de nombre
 */
@Entity
@Table(name = "loyalty_cards")
public class LoyaltyCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Vendeur propriétaire de la carte */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private VendorUser vendor;

    /** Nom du client fidèle */
    @NotBlank @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String customerName;

    /** Téléphone WhatsApp du client */
    @NotBlank @Size(max = 40)
    @Column(nullable = false, length = 40)
    private String customerPhone;

    /** Code unique à communiquer au client */
    @NotBlank @Size(max = 32)
    @Column(nullable = false, unique = true, length = 32)
    private String code;

    /** Réduction accordée en % (ex: 10 = -10%) */
    @Min(1) @Max(100)
    @Column(nullable = false)
    private int discountPercent = 10;

    /** Date d'expiration (null = pas d'expiration) */
    @Column
    private LocalDate expiresAt;

    /** Note interne du vendeur sur ce client */
    @Size(max = 300)
    @Column(length = 300)
    private String notes;

    /** Carte active ou non */
    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDate createdAt = LocalDate.now();

    // ─── Getters / Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }

    public VendorUser getVendor() { return vendor; }
    public void setVendor(VendorUser vendor) { this.vendor = vendor; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public int getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(int discountPercent) { this.discountPercent = discountPercent; }

    public LocalDate getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDate expiresAt) { this.expiresAt = expiresAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDate getCreatedAt() { return createdAt; }

    /** Vérifie si la carte est encore valide (non expirée + active) */
    public boolean isValid() {
        return active && (expiresAt == null || !expiresAt.isBefore(LocalDate.now()));
    }
}
