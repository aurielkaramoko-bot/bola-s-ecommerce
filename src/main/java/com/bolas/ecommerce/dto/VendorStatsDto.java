package com.bolas.ecommerce.dto;

import java.util.List;

/**
 * DTO contenant les statistiques vendeur.
 * PRO_LOCAL : stats basiques (CA, nb commandes, top produits)
 * PREMIUM   : + graphiques 6 mois, taux conversion
 */
public class VendorStatsDto {

    // --- Stats basiques (PRO_LOCAL + PREMIUM) ---
    private long monthlyRevenue;
    private long monthlyOrders;
    private long totalOrders;
    private long totalProducts;
    private List<TopProduct> topProducts;

    // --- Stats avancées (PREMIUM uniquement) ---
    private List<String> monthLabels;
    private List<Long> monthRevenues;
    private List<Long> monthOrderCounts;
    private double averageRating;
    private long reviewCount;

    // --- Getters / Setters ---

    public long getMonthlyRevenue() { return monthlyRevenue; }
    public void setMonthlyRevenue(long monthlyRevenue) { this.monthlyRevenue = monthlyRevenue; }

    public long getMonthlyOrders() { return monthlyOrders; }
    public void setMonthlyOrders(long monthlyOrders) { this.monthlyOrders = monthlyOrders; }

    public long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }

    public long getTotalProducts() { return totalProducts; }
    public void setTotalProducts(long totalProducts) { this.totalProducts = totalProducts; }

    public List<TopProduct> getTopProducts() { return topProducts; }
    public void setTopProducts(List<TopProduct> topProducts) { this.topProducts = topProducts; }

    public List<String> getMonthLabels() { return monthLabels; }
    public void setMonthLabels(List<String> monthLabels) { this.monthLabels = monthLabels; }

    public List<Long> getMonthRevenues() { return monthRevenues; }
    public void setMonthRevenues(List<Long> monthRevenues) { this.monthRevenues = monthRevenues; }

    public List<Long> getMonthOrderCounts() { return monthOrderCounts; }
    public void setMonthOrderCounts(List<Long> monthOrderCounts) { this.monthOrderCounts = monthOrderCounts; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public long getReviewCount() { return reviewCount; }
    public void setReviewCount(long reviewCount) { this.reviewCount = reviewCount; }

    /** Produit dans le top 5 */
    public static class TopProduct {
        private String name;
        private long orderCount;
        private long revenue;

        public TopProduct(String name, long orderCount, long revenue) {
            this.name = name;
            this.orderCount = orderCount;
            this.revenue = revenue;
        }

        public String getName() { return name; }
        public long getOrderCount() { return orderCount; }
        public long getRevenue() { return revenue; }
    }
}
