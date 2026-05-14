package com.bolas.ecommerce.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Compte de fidélité BOLA pour un client identifié par son téléphone.
 * 1 point = 100 CFA dépensés. Niveaux : Bronze/Argent/Or/Platine.
 * Clé : customerPhone (même structure que CustomerOrder).
 */
@Entity
@Table(name = "customer_loyalty_accounts")
public class CustomerLoyaltyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_phone", nullable = false, unique = true, length = 40)
    private String customerPhone;

    @Column(nullable = false)
    private int points = 0;

    @Column(name = "total_orders", nullable = false)
    private int totalOrders = 0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ─── Helpers ─────────────────────────────────────────────────────────────

    public String getNiveau() {
        if (points >= 1000) return "PLATINE";
        if (points >= 500)  return "OR";
        if (points >= 100)  return "ARGENT";
        return "BRONZE";
    }

    public String getNiveauEmoji() {
        return switch (getNiveau()) {
            case "PLATINE" -> "💎";
            case "OR"      -> "🥇";
            case "ARGENT"  -> "🥈";
            default        -> "🥉";
        };
    }

    public int getPointsToNextLevel() {
        return switch (getNiveau()) {
            case "BRONZE"  -> 100 - points;
            case "ARGENT"  -> 500 - points;
            case "OR"      -> 1000 - points;
            default        -> 0;
        };
    }

    public int getProgressPercent() {
        return switch (getNiveau()) {
            case "BRONZE"  -> Math.min(100, points);
            case "ARGENT"  -> Math.min(100, (points - 100) * 100 / 400);
            case "OR"      -> Math.min(100, (points - 500) * 100 / 500);
            default        -> 100;
        };
    }

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
