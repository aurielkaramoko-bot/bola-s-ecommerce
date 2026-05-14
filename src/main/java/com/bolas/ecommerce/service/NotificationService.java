package com.bolas.ecommerce.service;

import com.bolas.ecommerce.model.Notification;
import com.bolas.ecommerce.model.NotificationDestinataire;
import com.bolas.ecommerce.model.NotificationType;
import com.bolas.ecommerce.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service central de notifications in-app BOLA.
 *
 * Coexiste avec WhatsApp (MetaWhatsAppService) — les deux systèmes fonctionnent
 * en parallèle. Appeler ce service depuis :
 *  - OrderFlowService : nouvelle commande (vendeur), commande livrée (client)
 *  - SubscriptionExpiryService : abonnement expirant dans 3 jours
 *  - ReviewReportController : nouvel avis reçu (vendeur)
 *  - ChatController : nouveau message (destinataire)
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int RECENT_LIMIT = 5;

    private final NotificationRepository repo;

    public NotificationService(NotificationRepository repo) {
        this.repo = repo;
    }

    // ─── Création ─────────────────────────────────────────────────────────────

    /**
     * Envoie (crée) une notification interne.
     *
     * @param destinataireId   ID du destinataire
     * @param destinataireType VENDEUR / CLIENT / LIVREUR / ADMIN
     * @param type             COMMANDE / LIVRAISON / ABONNEMENT / AVIS / CHAT / SYSTEME
     * @param titre            Titre court (max 100 chars)
     * @param message          Corps court (max 160 chars — style SMS)
     * @param lienAction       URL de redirection au clic (peut être null)
     */
    @Transactional
    public void envoyer(Long destinataireId, NotificationDestinataire destinataireType,
                        NotificationType type, String titre, String message, String lienAction) {
        try {
            Notification n = new Notification();
            n.setDestinataireId(destinataireId);
            n.setDestinataireType(destinataireType);
            n.setType(type);
            n.setTitre(truncate(titre, 100));
            n.setMessage(truncate(message, 160));
            n.setLienAction(lienAction);
            repo.save(n);
        } catch (Exception e) {
            log.warn("⚠️ Notification non enregistrée (destinataire={}, type={}): {}",
                    destinataireId, type, e.getMessage());
        }
    }

    // ─── Lecture ──────────────────────────────────────────────────────────────

    /** 5 dernières notifications pour le panneau déroulant de la cloche */
    public List<Notification> getRecent(Long destinataireId, NotificationDestinataire type) {
        return repo.findByDestinataireIdAndDestinataireTypeOrderByCreatedAtDesc(
                destinataireId, type, PageRequest.of(0, RECENT_LIMIT));
    }

    /** Toutes les notifications pour la page /notifications */
    public List<Notification> getAll(Long destinataireId, NotificationDestinataire type) {
        return repo.findByDestinataireIdAndDestinataireTypeOrderByCreatedAtDesc(destinataireId, type);
    }

    /** Nombre de notifications non lues (pour le badge) */
    public long countUnread(Long destinataireId, NotificationDestinataire type) {
        return repo.countByDestinataireIdAndDestinataireTypeAndLueFalse(destinataireId, type);
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    /** Marquer une notification comme lue */
    @Transactional
    public void marquerLue(Long notifId) {
        repo.findById(notifId).ifPresent(n -> { n.setLue(true); repo.save(n); });
    }

    /** Marquer toutes les notifications comme lues */
    @Transactional
    public void marquerToutesLues(Long destinataireId, NotificationDestinataire type) {
        repo.markAllRead(destinataireId, type);
    }

    /** Supprimer une notification */
    @Transactional
    public void supprimer(Long notifId) {
        repo.deleteById(notifId);
    }

    // ─── Purge automatique ────────────────────────────────────────────────────

    /** Supprime chaque nuit les notifications lues de plus de 30 jours */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        repo.deleteOldRead(cutoff);
        log.info("🧹 Purge notifications lues > 30j effectuée");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
