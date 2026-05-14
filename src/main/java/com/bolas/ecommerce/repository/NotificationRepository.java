package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.Notification;
import com.bolas.ecommerce.model.NotificationDestinataire;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** 5 dernières notifs pour le panneau déroulant de la cloche */
    List<Notification> findByDestinataireIdAndDestinataireTypeOrderByCreatedAtDesc(
            Long destinataireId, NotificationDestinataire type, Pageable pageable);

    /** Toutes les notifs pour la page /notifications */
    List<Notification> findByDestinataireIdAndDestinataireTypeOrderByCreatedAtDesc(
            Long destinataireId, NotificationDestinataire type);

    /** Nombre de notifs non lues — utilisé par le badge et l'API /count */
    long countByDestinataireIdAndDestinataireTypeAndLueFalse(
            Long destinataireId, NotificationDestinataire type);

    /** Marquer toutes les notifs comme lues */
    @Modifying
    @Query("UPDATE Notification n SET n.lue = true WHERE n.destinataireId = :id AND n.destinataireType = :type")
    void markAllRead(@Param("id") Long destinataireId, @Param("type") NotificationDestinataire type);

    /** Purge automatique : supprimer les notifs lues de plus de 30 jours */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.lue = true AND n.createdAt < :cutoff")
    void deleteOldRead(@Param("cutoff") LocalDateTime cutoff);
}
