package com.bolas.ecommerce.dto;

import com.bolas.ecommerce.model.Notification;
import com.bolas.ecommerce.model.NotificationType;

import java.time.LocalDateTime;

/**
 * DTO de réponse pour les notifications — expose uniquement les champs publics,
 * sans exposer destinataireId ni les détails internes de l'entité.
 */
public class NotificationDto {

    private Long id;
    private String titre;
    private String message;
    private NotificationType type;
    private boolean lue;
    private LocalDateTime createdAt;
    private String lienAction;

    public static NotificationDto from(Notification n) {
        NotificationDto dto = new NotificationDto();
        dto.id         = n.getId();
        dto.titre      = n.getTitre();
        dto.message    = n.getMessage();
        dto.type       = n.getType();
        dto.lue        = n.isLue();
        dto.createdAt  = n.getCreatedAt();
        dto.lienAction = n.getLienAction();
        return dto;
    }

    public Long getId()              { return id; }
    public String getTitre()         { return titre; }
    public String getMessage()       { return message; }
    public NotificationType getType(){ return type; }
    public boolean isLue()           { return lue; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public String getLienAction()    { return lienAction; }
}
