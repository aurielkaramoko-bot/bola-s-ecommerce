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

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }
}
