package com.bolas.ecommerce.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Vendeur de confiance (sous-vendeur / employé) ajouté par le propriétaire de la boutique.
 * Le vendeur principal (VendorUser) crée et gère ses ShopSellers.
 * L'admin BOLA peut les voir en backstage mais ne les gère pas directement.
 */
@Entity
@Table(name = "shop_sellers",
       uniqueConstraints = @UniqueConstraint(columnNames = "username"))
public class ShopSeller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant de connexion unique */
    @NotBlank @Size(max = 100)
    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @NotBlank @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String passwordHash;

    /** Nom complet affiché */
    @NotBlank @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String fullName;

    /** Téléphone WhatsApp */
    @Size(max = 40)
    @Column(length = 40)
    private String phone;

    /** Email de contact */
    @Size(max = 200)
    @Column(length = 200)
    private String email;

    /** Boutique parente — le vendeur principal */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private VendorUser vendor;

    /** Actif / désactivé par le propriétaire */
    @Column(nullable = false)
    private boolean active = true;

    /** URL de la pièce d'identité (pour vérification) */
    @Size(max = 2000)
    @Column(name = "id_document_url", length = 2000)
    private String idDocumentUrl;

    /** Résultat du scan Vision API sur la pièce d'identité */
    @Column(name = "id_doc_verified")
    private Boolean idDocVerified;

    /** Date de création */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** Dernière connexion */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public VendorUser getVendor() { return vendor; }
    public void setVendor(VendorUser vendor) { this.vendor = vendor; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getIdDocumentUrl() { return idDocumentUrl; }
    public void setIdDocumentUrl(String idDocumentUrl) { this.idDocumentUrl = idDocumentUrl; }

    public Boolean getIdDocVerified() { return idDocVerified; }
    public void setIdDocVerified(Boolean idDocVerified) { this.idDocVerified = idDocVerified; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
