package com.bolas.ecommerce.dto;

import com.bolas.ecommerce.model.TrustLevel;

/**
 * DTO pour les alertes fraude affichées dans le dashboard admin.
 */
public class FraudAlertDto {

    private Long vendorId;
    private String vendorName;
    private String shopName;
    private int trustScore;
    private TrustLevel trustLevel;
    private String flagReason;
    private String email;
    private String phone;

    // Détails des signaux problématiques
    private double cancellationRate;
    private int reportCount;
    private int previousScore;
    private int productCount;
    private long accountAgeDays;

    public FraudAlertDto() {}

    public FraudAlertDto(Long vendorId, String vendorName, String shopName,
                          int trustScore, TrustLevel trustLevel, String flagReason) {
        this.vendorId = vendorId;
        this.vendorName = vendorName;
        this.shopName = shopName;
        this.trustScore = trustScore;
        this.trustLevel = trustLevel;
        this.flagReason = flagReason;
    }

    /** Icône d'alerte selon la sévérité */
    public String getAlertIcon() {
        if (trustScore < 15) return "🚨";
        if (trustScore < 30) return "⚠️";
        return "🔍";
    }

    /** Couleur CSS de l'alerte */
    public String getAlertColor() {
        if (trustScore < 15) return "#dc3545";
        if (trustScore < 30) return "#fd7e14";
        return "#ffc107";
    }

    // ─── Getters / Setters ────────────────────────────────────────────────

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public int getTrustScore() { return trustScore; }
    public void setTrustScore(int trustScore) { this.trustScore = trustScore; }

    public TrustLevel getTrustLevel() { return trustLevel; }
    public void setTrustLevel(TrustLevel trustLevel) { this.trustLevel = trustLevel; }

    public String getFlagReason() { return flagReason; }
    public void setFlagReason(String flagReason) { this.flagReason = flagReason; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public double getCancellationRate() { return cancellationRate; }
    public void setCancellationRate(double cancellationRate) { this.cancellationRate = cancellationRate; }

    public int getReportCount() { return reportCount; }
    public void setReportCount(int reportCount) { this.reportCount = reportCount; }

    public int getPreviousScore() { return previousScore; }
    public void setPreviousScore(int previousScore) { this.previousScore = previousScore; }

    public int getProductCount() { return productCount; }
    public void setProductCount(int productCount) { this.productCount = productCount; }

    public long getAccountAgeDays() { return accountAgeDays; }
    public void setAccountAgeDays(long accountAgeDays) { this.accountAgeDays = accountAgeDays; }
}
