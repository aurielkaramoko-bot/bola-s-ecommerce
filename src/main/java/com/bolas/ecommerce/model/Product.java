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

    /** Badge promo personnalisé (PRO/PREMIUM) — ex: "Soldes", "Fête des mères -20%" */
    @Size(max = 60)
    @Column(name = "promo_label", length = 60)
    private String promoLabel;

    /** Produit marqué comme Tendance (PRO/PREMIUM uniquement) */
    @Column(name = "trend_active", nullable = false)
    private boolean trendActive = false;

    /** Date d'expiration du badge Trend (auto-retiré après 14 jours) */
    @Column(name = "trend_expires_at")
    private java.time.LocalDateTime trendExpiresAt;

    /** URL de la vidéo du produit (YouTube ou TikTok). Optionnel. */
    @Size(max = 2000)
    @Column(name = "video_url", length = 2000)
    private String videoUrl;

    /** Tailles disponibles pour ce produit (CSV ex: "38,39,40") — null si sans taille */
    @Size(max = 200)
    @Column(name = "available_sizes", length = 200)
    private String availableSizes;

    /** Tailles en rupture de stock (CSV ex: "40,41") — sous-ensemble de availableSizes */
    @Size(max = 200)
    @Column(name = "out_of_stock_sizes", length = 200)
    private String outOfStockSizes;

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
        // Priorité : promotion individuelle > réduction boutique > prix normal
        if (isOnPromotion()) return promoPriceCfa;
        try {
            if (vendor != null && vendor.isShopDiscountActive()) {
                int discount = vendor.getShopDiscountPercent();
                return Math.max(0, priceCfa - (priceCfa * discount / 100));
            }
        } catch (Exception e) {
            // vendor non chargé (lazy) — on retourne le prix normal
        }
        return priceCfa;
    }

    /** Vérifie si le produit bénéficie de la réduction boutique (pas de promo individuelle) */
    public boolean hasShopDiscount() {
        try {
            return !isOnPromotion() && vendor != null && vendor.isShopDiscountActive();
        } catch (Exception e) {
            return false;
        }
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

    public String getPromoLabel() { return promoLabel; }
    public void setPromoLabel(String promoLabel) { this.promoLabel = promoLabel; }

    public boolean isTrendActive() { return trendActive; }
    public void setTrendActive(boolean trendActive) { this.trendActive = trendActive; }

    public java.time.LocalDateTime getTrendExpiresAt() { return trendExpiresAt; }
    public void setTrendExpiresAt(java.time.LocalDateTime trendExpiresAt) { this.trendExpiresAt = trendExpiresAt; }

    /** Vérifie si le badge Trend est actif et non expiré */
    public boolean isCurrentlyTrending() {
        if (!trendActive) return false;
        if (trendExpiresAt != null && trendExpiresAt.isBefore(java.time.LocalDateTime.now())) return false;
        return true;
    }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getAvailableSizes() { return availableSizes; }
    public void setAvailableSizes(String availableSizes) { this.availableSizes = availableSizes; }

    public String getOutOfStockSizes() { return outOfStockSizes; }
    public void setOutOfStockSizes(String outOfStockSizes) { this.outOfStockSizes = outOfStockSizes; }

    /** Liste parsée des tailles disponibles */
    public java.util.List<String> getSizeList() {
        return com.bolas.ecommerce.util.SizeUtil.parse(availableSizes);
    }

    /** Liste parsée des tailles en rupture */
    public java.util.List<String> getOutOfStockSizeList() {
        return com.bolas.ecommerce.util.SizeUtil.parse(outOfStockSizes);
    }

    public VendorUser getVendor() { return vendor; }
    public void setVendor(VendorUser vendor) { this.vendor = vendor; }

}
