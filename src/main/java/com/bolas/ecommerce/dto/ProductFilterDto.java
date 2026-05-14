package com.bolas.ecommerce.dto;

/**
 * DTO pour les filtres de recherche produits avancés.
 * Transmis du formulaire HTML vers ProductSpecification.
 */
public class ProductFilterDto {
    private String q;
    private Long categoryId;
    private Long priceMin;
    private Long priceMax;
    private String country;
    private String city;
    private Integer minRating;
    private boolean promoOnly;
    private String sortBy; // price_asc, price_desc, rating, newest

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Long getPriceMin() { return priceMin; }
    public void setPriceMin(Long priceMin) { this.priceMin = priceMin; }

    public Long getPriceMax() { return priceMax; }
    public void setPriceMax(Long priceMax) { this.priceMax = priceMax; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public Integer getMinRating() { return minRating; }
    public void setMinRating(Integer minRating) { this.minRating = minRating; }

    public boolean isPromoOnly() { return promoOnly; }
    public void setPromoOnly(boolean promoOnly) { this.promoOnly = promoOnly; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    /** Retourne true si au moins un filtre actif (hors tri) */
    public boolean hasActiveFilter() {
        return (q != null && !q.isBlank())
            || categoryId != null
            || priceMin != null || priceMax != null
            || (country != null && !country.isBlank())
            || (city != null && !city.isBlank())
            || (minRating != null && minRating > 0)
            || promoOnly;
    }
}
