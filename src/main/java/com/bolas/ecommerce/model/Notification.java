package com.bolas.ecommerce.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Notification interne BOLA — stockée en base, affichée dans la cloche navbar.
 * Coexiste avec WhatsApp : les deux systèmes fonctionnent en parallèle.
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notif_destinataire", columnList = "destinataire_id, destinataire_type"),
    @Index(name = "idx_notif_lue",          columnList = "lue, created_at")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID du destinataire (vendor id, customer id, etc.) */
    @Column(name = "destinataire_id", nullable = false)
    private Long destinataireId;

    @Enumerated(EnumType.STRING)
    @Column(name = "destinataire_type", nullable = false, length = 16)
    private NotificationDestinataire destinataireType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationType type;

    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String titre;

    /** Corps court, max 160 caractères (style SMS) */
    @Size(max = 160)
    @Column(nullable = false, length = 160)
    private String message;

    /** URL vers laquelle rediriger au clic (peut être null) */
    @Size(max = 500)
    @Column(name = "lien_action", length = 500)
    private String lienAction;

    @Column(nullable = false)
    private boolean lue = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public Long getDestinataireId() { return destinataireId; }
    public void setDestinataireId(Long destinataireId) { this.destinataireId = destinataireId; }

    public NotificationDestinataire getDestinataireType() { return destinataireType; }
    public void setDestinataireType(NotificationDestinataire destinataireType) { this.destinataireType = destinataireType; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getLienAction() { return lienAction; }
    public void setLienAction(String lienAction) { this.lienAction = lienAction; }

    public boolean isLue() { return lue; }
    public void setLue(boolean lue) { this.lue = lue; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
