package com.bolas.ecommerce.service;

import com.bolas.ecommerce.config.FirebaseConfig;
import com.bolas.ecommerce.model.FcmToken;
import com.bolas.ecommerce.model.NotificationDestinataire;
import com.bolas.ecommerce.repository.FcmTokenRepository;
import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service FCM — envoie des push notifications via Firebase Cloud Messaging.
 * Si Firebase n'est pas initialisé, les appels sont ignorés silencieusement.
 */
@Service
public class FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);

    private final FcmTokenRepository tokenRepo;

    public FcmService(FcmTokenRepository tokenRepo) {
        this.tokenRepo = tokenRepo;
    }

    // ─── Enregistrement token ────────────────────────────────────────────────

    @Transactional
    public void saveToken(Long userId, NotificationDestinataire userType, String fcmToken) {
        if (fcmToken == null || fcmToken.isBlank()) return;
        Optional<FcmToken> existing = tokenRepo.findByToken(fcmToken);
        if (existing.isPresent()) {
            existing.get().setLastUsedAt(Instant.now());
            tokenRepo.save(existing.get());
        } else {
            FcmToken t = new FcmToken();
            t.setUserId(userId);
            t.setUserType(userType);
            t.setToken(fcmToken);
            tokenRepo.save(t);
        }
    }

    // ─── Envoi par userId ─────────────────────────────────────────────────────

    public void notifyUser(Long userId, NotificationDestinataire userType,
                           String title, String body, String link) {
        if (!FirebaseConfig.isAvailable()) return;
        List<String> tokens = tokenRepo.findTokenStringsByUserIdAndUserType(userId, userType);
        tokens.forEach(t -> sendToToken(t, title, body, link));
    }

    // ─── Notifications métier ─────────────────────────────────────────────────

    public void notifyNouvelleCommande(Long vendorId, String trackingNumber, String customerName) {
        notifyUser(vendorId, NotificationDestinataire.VENDEUR,
                "Nouvelle commande !",
                customerName + " vient de commander — #" + trackingNumber,
                "/vendor/orders");
    }

    public void notifyCommandeConfirmee(Long customerId, String trackingNumber) {
        notifyUser(customerId, NotificationDestinataire.CLIENT,
                "Commande confirmée",
                "Votre commande #" + trackingNumber + " est en préparation.",
                "/tracking?trackingNumber=" + trackingNumber);
    }

    public void notifyEnLivraison(Long customerId, String trackingNumber, String courierName) {
        notifyUser(customerId, NotificationDestinataire.CLIENT,
                "Commande en livraison !",
                (courierName != null ? courierName + " livre " : "Livraison de ") + "#" + trackingNumber,
                "/tracking?trackingNumber=" + trackingNumber);
    }

    public void notifyLivre(Long customerId, Long vendorId, String trackingNumber) {
        notifyUser(customerId, NotificationDestinataire.CLIENT,
                "Commande livrée !",
                "Votre commande #" + trackingNumber + " a été livrée. Merci !",
                "/tracking?trackingNumber=" + trackingNumber);
        notifyUser(vendorId, NotificationDestinataire.VENDEUR,
                "Commande livrée",
                "#" + trackingNumber + " a été livrée avec succès.",
                "/vendor/orders");
    }

    public void notifyNouveauMessage(Long recipientId, NotificationDestinataire recipientType,
                                     String senderName) {
        notifyUser(recipientId, recipientType,
                "Nouveau message",
                senderName + " vous a envoyé un message",
                "/vendor/messages");
    }

    public void notifyAbonnementActive(Long vendorId, String planName) {
        notifyUser(vendorId, NotificationDestinataire.VENDEUR,
                "Abonnement activé !",
                "Votre plan " + planName + " est maintenant actif.",
                "/vendor/dashboard");
    }

    public void notifyAbonnementRefuse(Long vendorId) {
        notifyUser(vendorId, NotificationDestinataire.VENDEUR,
                "Demande refusée",
                "Votre demande d'abonnement a été refusée. Contactez-nous.",
                "/vendor/dashboard");
    }

    public void notifyNouvelleDemandeAbonnement(Long adminId, String vendorName, String plan) {
        notifyUser(adminId, NotificationDestinataire.ADMIN,
                "Demande abonnement",
                vendorName + " demande le plan " + plan,
                "/admin/vendors");
    }

    public void notifyReponseCommentaire(Long authorId, String responderName, String productName) {
        notifyUser(authorId, NotificationDestinataire.CLIENT,
                "Réponse à votre commentaire",
                responderName + " a répondu sur " + productName,
                "/");
    }

    /**
     * Notification livreur : une commande lui a été assignée.
     * Utilisé quand le vendeur clique "Envoyer en livraison" et qu'un livreur est assigné.
     */
    public void notifyLivreurAssigne(Long customerId, String trackingNumber,
                                     String livreurName, String vendorName) {
        // Notifie le client que son livreur est en route
        notifyUser(customerId, NotificationDestinataire.CLIENT,
                "Livreur en route !",
                (livreurName != null ? livreurName : "Un livreur") + " va livrer votre commande #" + trackingNumber,
                "/tracking?trackingNumber=" + trackingNumber);
    }

    /**
     * Notification client : commande confirmée — reçu en production.
     */
    public void notifyCommandeRecue(Long customerId, String trackingNumber, String vendorName) {
        notifyUser(customerId, NotificationDestinataire.CLIENT,
                "Commande confirmée par " + vendorName,
                "Votre commande #" + trackingNumber + " est en préparation. 🎉",
                "/tracking?trackingNumber=" + trackingNumber);
    }

    // ─── Envoi direct par token ───────────────────────────────────────────────

    public void sendToToken(String token, String title, String body, String link) {
        if (!FirebaseConfig.isAvailable() || token == null || token.isBlank()) return;
        try {
            Message msg = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(truncate(title, 100))
                            .setBody(truncate(body, 200))
                            .build())
                    .setWebpushConfig(WebpushConfig.builder()
                            .setFcmOptions(WebpushFcmOptions.builder()
                                    .setLink(link != null ? link : "/")
                                    .build())
                            .build())
                    .build();
            FirebaseMessaging.getInstance().send(msg);
        } catch (FirebaseMessagingException e) {
            String code = e.getMessagingErrorCode() != null ? e.getMessagingErrorCode().name() : "";
            if ("UNREGISTERED".equals(code) || "INVALID_ARGUMENT".equals(code)) {
                tokenRepo.findByToken(token).ifPresent(tokenRepo::delete);
                log.debug("FCM token supprimé (invalide): {}...", token.substring(0, Math.min(20, token.length())));
            } else {
                log.warn("FCM erreur: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.warn("FCM erreur inattendue: {}", e.getMessage());
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
