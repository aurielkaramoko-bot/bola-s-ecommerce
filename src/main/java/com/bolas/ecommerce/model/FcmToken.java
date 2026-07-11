package com.bolas.ecommerce.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

/**
 * Token FCM d'un utilisateur (client, vendeur, livreur).
 * Un utilisateur peut avoir plusieurs tokens (multi-appareils).
 */
@Entity
@Table(name = "fcm_tokens",
       uniqueConstraints = @UniqueConstraint(columnNames = "token"))
public class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Type d'utilisateur */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationDestinataire userType;

    /** ID de l'utilisateur */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Token FCM de l'appareil */
    @JsonIgnore
    @Column(nullable = false, length = 500)
    private String token;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_used_at")
    private Instant lastUsedAt = Instant.now();

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public NotificationDestinataire getUserType() { return userType; }
    public void setUserType(NotificationDestinataire userType) { this.userType = userType; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}
