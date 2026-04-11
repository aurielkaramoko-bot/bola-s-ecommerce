package com.bolas.ecommerce.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.persistence.Transient;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false)
    private String name;

    @Size(max = 8000)
    @Column(length = 8000)
    private String description;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Long priceCfa;

    /** Prix promotionnel optionnel (CFA). Colonne SQL : prix_promo */
    @Min(0)
    @Column(name = "prix_promo")
    private Long promoPriceCfa;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Size(max = 2000)
    @Column(length = 2000)
    private String imageUrl;

    @Column(nullable = false)
    private boolean available = true;

    @Column(nullable = false)
    private boolean deliveryAvailable = true;

    @Min(0)
    @Column(nullable = false)
    private long deliveryPriceCfa;

    @Column(nullable = false)
    private boolean featured;

    /** Produit sponsorisé (PREMIUM uniquement) — apparaît en tête de recherche avec badge */
    @Column(nullable = false)
    private boolean sponsored = false;

    /** Stock limité — affiche un badge d'urgence sur la fiche produit */
    @Column(nullable = false)
    private boolean limitedStock = false;

    /** URL de la vidéo du produit (YouTube ou TikTok). Optionnel. */
    @Size(max = 2000)
    @Column(name = "video_url", length = 2000)
    private String videoUrl;

    /**
     * Vendeur propriétaire de ce produit.
     * null = produit appartenant directement à BOLA (ajouté par admin).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private VendorUser vendor;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getPriceCfa() {
        return priceCfa;
    }

    public void setPriceCfa(Long priceCfa) {
        this.priceCfa = priceCfa;
    }

    public Long getPromoPriceCfa() {
        return promoPriceCfa;
    }

    public void setPromoPriceCfa(Long promoPriceCfa) {
        this.promoPriceCfa = promoPriceCfa;
    }

    public boolean isOnPromotion() {
        return promoPriceCfa != null && promoPriceCfa >= 0 && promoPriceCfa < priceCfa;
    }

    public long getEffectivePriceCfa() {
        return isOnPromotion() ? promoPriceCfa : priceCfa;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isDeliveryAvailable() {
        return deliveryAvailable;
    }

    public void setDeliveryAvailable(boolean deliveryAvailable) {
        this.deliveryAvailable = deliveryAvailable;
    }

    public long getDeliveryPriceCfa() {
        return deliveryPriceCfa;
    }

    public void setDeliveryPriceCfa(long deliveryPriceCfa) {
        this.deliveryPriceCfa = deliveryPriceCfa;
    }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public boolean isSponsored() { return sponsored; }
    public void setSponsored(boolean sponsored) { this.sponsored = sponsored; }

    public boolean isLimitedStock() { return limitedStock; }
    public void setLimitedStock(boolean limitedStock) { this.limitedStock = limitedStock; }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public VendorUser getVendor() { return vendor; }
    public void setVendor(VendorUser vendor) { this.vendor = vendor; }

}
