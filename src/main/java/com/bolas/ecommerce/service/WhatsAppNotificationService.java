package com.bolas.ecommerce.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Génère des liens WhatsApp wa.me prêts à cliquer pour notifier l'admin.
 *
 * Pour l'instant, pas d'API WhatsApp Business (payante).
 * On construit un lien wa.me?text=... que l'admin peut cliquer ou
 * qu'un webhook pourrait ouvrir automatiquement.
 */
@Service
public class WhatsAppNotificationService {

    @Value("${whatsapp.number:22870099525}")
    private String adminWhatsApp;

    @Value("${bolas.shop.name:BOLA}")
    private String shopName;

    /**
     * Construit le lien WhatsApp pour notifier l'admin d'une nouvelle inscription vendeur.
     */
    public String buildVendorRegistrationLink(String shopNameVendor, String username,
                                               String phone, String categories,
                                               String nicheRequest) {
        StringBuilder sb = new StringBuilder();
        sb.append("🆕 *Nouvelle demande de boutique sur ").append(shopName).append("* !\n\n");
        sb.append("🏪 *Boutique :* ").append(shopNameVendor).append("\n");
        sb.append("👤 *Identifiant :* ").append(username).append("\n");
        sb.append("📞 *Téléphone :* ").append(phone).append("\n");
        if (categories != null && !categories.isBlank()) {
            sb.append("🏷️ *Catégories :* ").append(categories).append("\n");
        }
        if (nicheRequest != null && !nicheRequest.isBlank()) {
            sb.append("💡 *Niche demandée :* ").append(nicheRequest).append("\n");
        }
        sb.append("\n→ Validez depuis l'admin BOLA");

        return "https://wa.me/" + adminWhatsApp + "?text="
                + URLEncoder.encode(sb.toString(), StandardCharsets.UTF_8);
    }

    /**
     * Construit le lien WhatsApp pour notifier l'admin d'un signalement.
     */
    public String buildReportLink(String targetType, String targetName,
                                   String reporterEmail, String reason) {
        StringBuilder sb = new StringBuilder();
        sb.append("🚩 *Signalement sur ").append(shopName).append("* !\n\n");
        sb.append("📦 *Type :* ").append(targetType).append("\n");
        sb.append("📋 *Cible :* ").append(targetName).append("\n");
        sb.append("📧 *Signalé par :* ").append(reporterEmail).append("\n");
        sb.append("💬 *Raison :* ").append(reason).append("\n");
        sb.append("\n→ Traitez depuis l'admin BOLA");

        return "https://wa.me/" + adminWhatsApp + "?text="
                + URLEncoder.encode(sb.toString(), StandardCharsets.UTF_8);
    }

    /**
     * Construit un lien WhatsApp de confirmation pour le vendeur approuvé.
     */
    public String buildVendorApprovalLink(String vendorPhone, String shopNameVendor) {
        String msg = "✅ Félicitations ! Votre boutique *" + shopNameVendor
                + "* a été approuvée sur *" + shopName + "* !\n\n"
                + "Connectez-vous à votre espace vendeur pour commencer à publier vos produits.\n"
                + "📱 Équipe " + shopName;

        return "https://wa.me/" + vendorPhone + "?text="
                + URLEncoder.encode(msg, StandardCharsets.UTF_8);
    }

    public String getAdminWhatsApp() { return adminWhatsApp; }
}
