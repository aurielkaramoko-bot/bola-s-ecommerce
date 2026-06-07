package com.bolas.ecommerce.model;

import jakarta.persistence.*;

/**
 * Association entre un vendeur et un livreur.
 * Permet au vendeur de gérer sa liste de livreurs.
 */
@Entity
@Table(name = "vendor_livreurs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"vendor_id", "livreur_id"}))
public class VendorLivreur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private VendorUser vendor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "livreur_id", nullable = false)
    private Livreur livreur;

    /** Actif = le livreur peut recevoir des commandes de ce vendeur */
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public VendorUser getVendor() { return vendor; }
    public void setVendor(VendorUser vendor) { this.vendor = vendor; }
    public Livreur getLivreur() { return livreur; }
    public void setLivreur(Livreur livreur) { this.livreur = livreur; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
}
