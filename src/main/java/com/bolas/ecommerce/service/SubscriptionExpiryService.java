package com.bolas.ecommerce.service;

import com.bolas.ecommerce.model.VendorUser;
import com.bolas.ecommerce.repository.VendorUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SubscriptionExpiryService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionExpiryService.class);

    @Autowired
    private VendorUserRepository vendorUserRepository;

    @Autowired(required = false)
    private MetaWhatsAppService metaWhatsAppService;

    @Value("${app.whatsapp.enabled:true}")
    private boolean whatsAppEnabled;

    /**
     * Vérifie quotidiennement (à 8h du matin) si des abonnements expirent bientôt
     * Envoie une notification WhatsApp aux vendeurs concernés
     */
    @Scheduled(cron = "0 0 8 * * *") // Tous les jours à 8:00
    public void checkAndNotifyExpiringSubscriptions() {
        log.info("🔔 Démarrage de la vérification des abonnements expirant...");
        
        try {
            LocalDate today = LocalDate.now();
            LocalDate in5Days = today.plusDays(5);
            
            // Récupérer les vendeurs dont l'abonnement expire dans 5 jours
            List<VendorUser> expiringSubscriptions = vendorUserRepository.findBySubscriptionExpiresAtBetween(
                today.plusDays(1), 
                in5Days
            );
            
            log.info("✅ {} vendeurs avec abonnement expirant trouvés", expiringSubscriptions.size());
            
            for (VendorUser vendor : expiringSubscriptions) {
                notifyVendorAboutExpiringSubscription(vendor);
            }
            
            // Notifier aussi les abonnements expirés aujourd'hui
            List<VendorUser> todayExpiring = vendorUserRepository.findBySubscriptionExpiresAt(today);
            for (VendorUser vendor : todayExpiring) {
                notifyVendorAboutExpiredSubscription(vendor);
            }
            
            log.info("✅ Vérification des abonnements terminée avec succès");
        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification des abonnements expirant", e);
        }
    }

    /**
     * Envoie une notification à un vendeur pour avertir que son abonnement expire bientôt
     */
    private void notifyVendorAboutExpiringSubscription(VendorUser vendor) {
        try {
            if (vendor.getPhone() == null || vendor.getPhone().isBlank()) {
                log.warn("⚠️ Numéro de téléphone absent pour le vendeur {}", vendor.getDisplayName());
                return;
            }
            
            long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDate.now(), 
                vendor.getSubscriptionExpiresAt()
            );
            
            String message = String.format(
                "📢 *Notification BOLA*\n\n" +
                "Bonjour %s! 👋\n\n" +
                "Votre abonnement *%s* expire dans *%d jour(s)* (le %s).\n\n" +
                "Pour continuer à bénéficier de tous vos avantages et rester visible dans notre application, " +
                "veuillez renouveler votre abonnement dès que possible.\n\n" +
                "💰 Renouvellement rapide: https://bolas.cm\n\n" +
                "Merci de votre confiance! 🙏",
                vendor.getDisplayName(),
                vendor.getPlan() != null ? vendor.getPlan().name() : "GRATUIT",
                daysUntilExpiry,
                vendor.getSubscriptionExpiresAt()
            );
            
            if (whatsAppEnabled && metaWhatsAppService != null) {
                try {
                    metaWhatsAppService.sendText(vendor.getPhone(), message);
                    log.info("✅ Notification d'expiration envoyée à {} (expires le {})", 
                        vendor.getDisplayName(), vendor.getSubscriptionExpiresAt());
                } catch (Exception e) {
                    log.warn("⚠️ Impossible d'envoyer WhatsApp à {}: {}", vendor.getDisplayName(), e.getMessage());
                }
            } else {
                log.info("📌 WhatsApp désactivé/non disponible pour {}", vendor.getDisplayName());
            }
        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement de la notification d'expiration pour {}", 
                vendor.getDisplayName(), e);
        }
    }

    /**
     * Envoie une notification à un vendeur quand son abonnement a expiré
     */
    private void notifyVendorAboutExpiredSubscription(VendorUser vendor) {
        try {
            if (vendor.getPhone() == null || vendor.getPhone().isBlank()) {
                log.warn("⚠️ Numéro de téléphone absent pour le vendeur {}", vendor.getDisplayName());
                return;
            }
            
            String message = String.format(
                "⚠️ *ALERTE - Abonnement Expiré*\n\n" +
                "Bonjour %s! 🚨\n\n" +
                "Attention! Votre abonnement BOLA a *expiré aujourd'hui*.\n\n" +
                "Votre boutique est actuellement *suspendue* dans notre application. " +
                "Pour réactiver votre présence et continuer vos ventes, renouvellement urgent requis.\n\n" +
                "⏰ Agir maintenant: https://bolas.cm\n\n" +
                "En cas de besoin, contactez notre équipe support.\n\n" +
                "À bientôt! 💪",
                vendor.getDisplayName()
            );
            
            if (whatsAppEnabled && metaWhatsAppService != null) {
                try {
                    metaWhatsAppService.sendText(vendor.getPhone(), message);
                    log.info("🚨 Notification d'expiration (AUJOURD'HUI) envoyée à {}", 
                        vendor.getDisplayName());
                } catch (Exception e) {
                    log.warn("⚠️ Impossible d'envoyer l'alerte WhatsApp à {}: {}", vendor.getDisplayName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de la notification d'expiration pour {}", 
                vendor.getDisplayName(), e);
        }
    }

    /**
     * Méthode auxiliaire pour envoyer une notification manuelle de test
     */
    public void sendTestNotification(long vendorId) {
        try {
            VendorUser vendor = vendorUserRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendeur non trouvé"));
            
            if (vendor.getSubscriptionExpiresAt() == null) {
                log.warn("❌ Pas d'abonnement à notifier pour {}", vendor.getDisplayName());
                return;
            }
            
            long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDate.now(), 
                vendor.getSubscriptionExpiresAt()
            );
            
            if (daysUntilExpiry <= 0) {
                notifyVendorAboutExpiredSubscription(vendor);
            } else {
                notifyVendorAboutExpiringSubscription(vendor);
            }
            
            log.info("✅ Notification de test envoyée au vendeur {}", vendor.getDisplayName());
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de la notification de test", e);
        }
    }
}
