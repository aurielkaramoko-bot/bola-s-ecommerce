package com.bolas.ecommerce.model;

import jakarta.persistence.*;

/**
 * Table de jointure : lie un vendeur à ses catégories autorisées.
 * Un vendeur ne peut publier des produits que dans ses catégories assignées.
 */
@Entity
@Table(name = "vendor_categories",
       uniqueConstraints = @UniqueConstraint(columnNames = {"vendor_id", "category_id"}))
public class VendorCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private VendorUser vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    public VendorCategory() {}

    public VendorCategory(VendorUser vendor, Category category) {
        this.vendor = vendor;
        this.category = category;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public VendorUser getVendor() { return vendor; }
    public void setVendor(VendorUser vendor) { this.vendor = vendor; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
}
