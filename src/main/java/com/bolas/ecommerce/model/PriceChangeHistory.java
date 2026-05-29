package com.bolas.ecommerce.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Historique des changements de prix des plans d'abonnement.
 */
@Entity
@Table(name = "price_change_history")
public class PriceChangeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_name", nullable = false, length = 20)
    private String planName;

    @Column(name = "old_price", nullable = false)
    private int oldPrice;

    @Column(name = "new_price", nullable = false)
    private int newPrice;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    public PriceChangeHistory() {}

    public PriceChangeHistory(String planName, int oldPrice, int newPrice) {
        this.planName = planName;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.changedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public int getOldPrice() { return oldPrice; }
    public void setOldPrice(int oldPrice) { this.oldPrice = oldPrice; }
    public int getNewPrice() { return newPrice; }
    public void setNewPrice(int newPrice) { this.newPrice = newPrice; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }

    /** Formaté pour l'affichage */
    public String getChangedAtFormatted() {
        if (changedAt == null) return "—";
        return changedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}
