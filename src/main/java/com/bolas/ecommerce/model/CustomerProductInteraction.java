package com.bolas.ecommerce.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Trace les interactions client-produit pour alimenter le moteur de recommandation IA.
 * Chaque interaction (vue, ajout panier, achat, avis) est enregistrée avec un poids
 * permettant de construire le profil d'intérêt du client.
 */
@Entity
@Table(name = "customer_product_interactions",
       indexes = {
           @Index(name = "idx_cpi_customer", columnList = "customer_id"),
           @Index(name = "idx_cpi_product", columnList = "product_id"),
           @Index(name = "idx_cpi_customer_type", columnList = "customer_id, interaction_type"),
           @Index(name = "idx_cpi_created", columnList = "created_at")
       })
public class CustomerProductInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @NotNull
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false, length = 20)
    private InteractionType interactionType;

    /** Poids de l'interaction (copié depuis InteractionType au moment de l'enregistrement) */
    @Min(0)
    @Column(nullable = false)
    private int weight;

    /** ID de la catégorie du produit (dénormalisé pour les requêtes de recommandation) */
    @Column(name = "category_id")
    private Long categoryId;

    /** ID du vendeur du produit (dénormalisé pour les requêtes de recommandation) */
    @Column(name = "vendor_id")
    private Long vendorId;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // ─── Constructeurs ────────────────────────────────────────────────────

    public CustomerProductInteraction() {}

    public CustomerProductInteraction(Long customerId, Long productId,
                                       InteractionType interactionType,
                                       Long categoryId, Long vendorId) {
        this.customerId = customerId;
        this.productId = productId;
        this.interactionType = interactionType;
        this.weight = interactionType.getWeight();
        this.categoryId = categoryId;
        this.vendorId = vendorId;
        this.createdAt = Instant.now();
    }

    // ─── Getters / Setters ────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public InteractionType getInteractionType() { return interactionType; }
    public void setInteractionType(InteractionType interactionType) {
        this.interactionType = interactionType;
        this.weight = interactionType.getWeight();
    }

    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
