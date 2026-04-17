package com.bolas.ecommerce.service;

import com.bolas.ecommerce.model.CustomerOrder;
import com.bolas.ecommerce.model.OrderStatus;
import com.bolas.ecommerce.model.VendorPlan;
import com.bolas.ecommerce.model.VendorUser;
import com.bolas.ecommerce.repository.CustomerOrderRepository;
import com.bolas.ecommerce.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gère le flow de validation des commandes.
 *
 * Logique de routage par plan :
 *  - GRATUIT   → admin gère tout (PENDING → CONFIRMED → READY → IN_DELIVERY)
 *  - PRO/PREMIUM → vendeur gère PENDING → CONFIRMED → READY, admin dispatche le livreur
 *
 * Notifications WhatsApp automatiques via Meta Cloud API à chaque étape.
 */
@Service
public class OrderFlowService {

    private static final Logger log = LoggerFactory.getLogger(OrderFlowService.class);

    private final CustomerOrderRepository orderRepository;
    private final AuditLogService auditLogService;
    private final MetaWhatsAppService metaWhatsApp;
    private final ProductRepository productRepository;

    @Value("${whatsapp.number}")
    private String shopWhatsapp;

    @Value("${bolas.shop.name:BOLA}")
    private String shopName;

    // Seuils de fidélité (articles cumulés chez un même vendeur)
    private static final int LOYALTY_THRESHOLD_1 = 10;
    private static final int LOYALTY_THRESHOLD_2 = 20;

    public OrderFlowService(CustomerOrderRepository orderRepository,
                            AuditLogService auditLogService,
                            MetaWhatsAppService metaWhatsApp,
                            ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.auditLogService = auditLogService;
        this.metaWhatsApp = metaWhatsApp;
        this.productRepository = productRepository;
    }

    // ─── Admin confirme une commande PENDING → CONFIRMED ─────────────────────

    /**
     * Admin valide la commande PENDING → CONFIRMED.
     * Envoie une notification WhatsApp automatique au vendeur PRO/PREMIUM.
     * Retourne le lien WhatsApp pour fallback (wa.me).
     */
    @Transactional
    public String confirmOrder(CustomerOrder order, String vendorPhone, String appBaseUrl) {
        log.info("confirmOrder: id={} status={}", order.getId(), order.getStatus());
        try {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            auditLogService.orderStatusChanged(order.getId(), order.getTrackingNumber(), "CONFIRMED");
        } catch (Exception e) {
            log.error("confirmOrder FAILED: {}", e.getMessage(), e);
            throw e;
        }

        // Notification WhatsApp Meta automatique au vendeur PRO/PREMIUM
        VendorUser vendor = order.getVendor();
        if (vendor != null && vendor.canManageOrders()
                && vendor.getPhone() != null && !vendor.getPhone().isBlank()) {
            try {
                String msg = "🛍️ Commande confirmée par l'admin " + shopName + " !\n\n"
                        + "📦 N° : " + order.getTrackingNumber() + "\n"
                        + "👤 Client : " + order.getCustomerName() + "\n"
                        + "📞 Tél : " + order.getCustomerPhone() + "\n"
                        + "📍 Option : " + deliveryLabel(order) + "\n"
                        + "💰 Total : " + order.getTotalAmountCfa() + " CFA\n\n"
                        + "→ Préparez la commande : " + appBaseUrl + "/vendor/orders";
                metaWhatsApp.sendText(vendor.getPhone(), msg);
                log.info("✅ WhatsApp auto envoyé au vendeur {} pour commande {}", vendor.getDisplayName(), order.getTrackingNumber());
            } catch (Exception e) {
                log.warn("⚠️ WhatsApp vendeur échoué : {}", e.getMessage());
            }
        }

        // Notification WhatsApp Meta au client
        notifyClientOrderConfirmed(order, appBaseUrl);

        // Vérification fidélité
        checkAndNotifyLoyalty(order);

        // Fallback wa.me link
        String msg = "🛍️ Nouvelle commande à préparer !\n\n"
                + "N° : " + order.getTrackingNumber() + "\n"
                + "Client : " + order.getCustomerName() + "\n"
                + "Tél : " + order.getCustomerPhone() + "\n"
                + "Option : " + deliveryLabel(order) + "\n"
                + "Montant : " + order.getTotalAmountCfa() + " CFA\n\n"
                + "Connectez-vous pour préparer : " + appBaseUrl + "/vendor/orders";
        return waLink(vendorPhone, msg);
    }

    // ─── Vendeur PRO/PREMIUM confirme PENDING → CONFIRMED ────────────────────

    /**
     * Vendeur PRO/PREMIUM confirme lui-même une commande PENDING → CONFIRMED.
     * L'admin est notifié, le client aussi.
     */
    @Transactional
    public void vendorConfirmOrder(CustomerOrder order, VendorUser vendor, String appBaseUrl) {
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        auditLogService.orderStatusChanged(order.getId(), order.getTrackingNumber(), "CONFIRMED (vendeur)");

        // Notifier l'admin via WhatsApp
        try {
            String adminMsg = "✅ Commande confirmée par le vendeur !\n\n"
                    + "🏪 Vendeur : " + vendor.getDisplayName() + "\n"
                    + "📦 N° : " + order.getTrackingNumber() + "\n"
                    + "👤 Client : " + order.getCustomerName() + "\n"
                    + "💰 Total : " + order.getTotalAmountCfa() + " CFA\n\n"
                    + "→ Le vendeur prépare la commande.";
            metaWhatsApp.sendText(shopWhatsapp, adminMsg);
        } catch (Exception e) {
            log.warn("⚠️ WhatsApp admin échoué : {}", e.getMessage());
        }

        // Notifier le client
        notifyClientOrderConfirmed(order, appBaseUrl);
    }

    // ─── Vendeur marque CONFIRMED → READY ────────────────────────────────────

    /**
     * Vendeur valide la préparation CONFIRMED → READY.
     * Envoie une notification WhatsApp automatique à l'admin.
     * Retourne le lien WhatsApp pour fallback.
     */
    @Transactional
    public String markReady(CustomerOrder order, String appBaseUrl) {
        order.setStatus(OrderStatus.READY);
        orderRepository.save(order);
        auditLogService.orderStatusChanged(order.getId(), order.getTrackingNumber(), "READY");

        // Notification WhatsApp Meta à l'admin
        try {
            String adminMsg = "✅ Commande prête !\n\n"
                    + "📦 N° : " + order.getTrackingNumber() + "\n"
                    + "🏪 Vendeur : " + (order.getVendor() != null ? order.getVendor().getDisplayName() : shopName) + "\n"
                    + "👤 Client : " + order.getCustomerName() + "\n"
                    + "📍 Option : " + deliveryLabel(order) + "\n\n"
                    + "→ Assignez un livreur depuis l'admin BOLA";
            metaWhatsApp.sendText(shopWhatsapp, adminMsg);
            log.info("✅ WhatsApp auto admin : commande {} prête", order.getTrackingNumber());
        } catch (Exception e) {
            log.warn("⚠️ WhatsApp admin (ready) échoué : {}", e.getMessage());
        }

        // Notifier le client que sa commande est prête
        try {
            if (order.getCustomerPhone() != null && !order.getCustomerPhone().isBlank()) {
                String clientMsg = "📦 Bonjour " + order.getCustomerName() + " !\n\n"
                        + "Votre commande " + shopName + " N° " + order.getTrackingNumber()
                        + " est prête et sera bientôt expédiée.\n\n"
                        + "Nous vous notifierons dès le départ du livreur. 🚚";
                metaWhatsApp.sendText(order.getCustomerPhone(), clientMsg);
            }
        } catch (Exception e) {
            log.warn("⚠️ WhatsApp client (ready) échoué : {}", e.getMessage());
        }

        // Fallback wa.me link
        String msg = "✅ Commande prête !\n\n"
                + "N° : " + order.getTrackingNumber() + "\n"
                + "Client : " + order.getCustomerName() + "\n"
                + "Option : " + deliveryLabel(order) + "\n\n"
                + "Voir dans l'admin : " + appBaseUrl + "/admin/orders";
        return waLink(shopWhatsapp, msg);
    }

    // ─── Admin envoie en livraison READY → IN_DELIVERY ───────────────────────

    /**
     * Admin informe le client que sa commande est prête / en livraison.
     * Envoie une notification WhatsApp automatique au client.
     */
    @Transactional
    public String notifyClientReady(CustomerOrder order, String appBaseUrl) {
        order.setStatus(OrderStatus.IN_DELIVERY);
        orderRepository.save(order);
        auditLogService.orderStatusChanged(order.getId(), order.getTrackingNumber(), "IN_DELIVERY");

        String trackUrl = appBaseUrl + "/tracking?trackingNumber=" + order.getTrackingNumber();

        // Notification WhatsApp Meta automatique au client
        try {
            if (order.getCustomerPhone() != null && !order.getCustomerPhone().isBlank()) {
                String clientMsg = "🚚 Bonjour " + order.getCustomerName() + " !\n\n"
                        + "Votre commande " + shopName + " est "
                        + (isHomeDelivery(order) ? "en cours de livraison" : "prête pour le retrait") + " !\n\n"
                        + "📦 N° de suivi : " + order.getTrackingNumber() + "\n"
                        + "🔍 Suivre votre commande : " + trackUrl;
                if (order.getAssignedCourierName() != null) {
                    clientMsg += "\n👤 Livreur : " + order.getAssignedCourierName();
                    if (order.getAssignedCourierPhone() != null) {
                        clientMsg += " (📞 " + order.getAssignedCourierPhone() + ")";
                    }
                }
                metaWhatsApp.sendText(order.getCustomerPhone(), clientMsg);
                log.info("✅ WhatsApp auto client : commande {} en livraison", order.getTrackingNumber());
            }
        } catch (Exception e) {
            log.warn("⚠️ WhatsApp client (livraison) échoué : {}", e.getMessage());
        }

        // Notifier le vendeur PRO/PREMIUM que la commande est en livraison
        VendorUser vendor = order.getVendor();
        if (vendor != null && vendor.canManageOrders()
                && vendor.getPhone() != null && !vendor.getPhone().isBlank()) {
            try {
                String vendorMsg = "🚚 Commande " + order.getTrackingNumber() + " en livraison !\n\n"
                        + "👤 Client : " + order.getCustomerName() + "\n"
                        + (order.getAssignedCourierName() != null
                                ? "📱 Livreur : " + order.getAssignedCourierName() + "\n" : "")
                        + "\n→ Suivi en temps réel depuis votre dashboard vendeur.";
                metaWhatsApp.sendText(vendor.getPhone(), vendorMsg);
            } catch (Exception e) {
                log.warn("⚠️ WhatsApp vendeur (livraison) échoué : {}", e.getMessage());
            }
        }

        // Fallback wa.me link
        String msg = "Bonjour " + order.getCustomerName() + " 👋\n\n"
                + "Votre commande " + shopName + " est prête"
                + (isHomeDelivery(order) ? " et en cours de livraison 🚚" : " pour le retrait 🏪") + " !\n\n"
                + "📦 N° de suivi : " + order.getTrackingNumber() + "\n"
                + "🔍 Suivre votre commande : " + trackUrl;
        return waLink(order.getCustomerPhone(), msg);
    }

    // ─── Notification commande annulée ───────────────────────────────────────

    /**
     * Notifie le vendeur PRO/PREMIUM qu'une commande a été annulée.
     */
    public void notifyVendorOrderCancelled(CustomerOrder order) {
        VendorUser vendor = order.getVendor();
        if (vendor != null && vendor.canManageOrders()
                && vendor.getPhone() != null && !vendor.getPhone().isBlank()) {
            try {
                String msg = "❌ Commande annulée\n\n"
                        + "📦 N° : " + order.getTrackingNumber() + "\n"
                        + "👤 Client : " + order.getCustomerName() + "\n"
                        + "💰 Montant : " + order.getTotalAmountCfa() + " CFA\n\n"
                        + "Cette commande a été annulée par l'admin " + shopName + ".";
                metaWhatsApp.sendText(vendor.getPhone(), msg);
                log.info("✅ WhatsApp auto vendeur : commande {} annulée", order.getTrackingNumber());
            } catch (Exception e) {
                log.warn("⚠️ WhatsApp vendeur (annulation) échoué : {}", e.getMessage());
            }
        }
    }

    // ─── Notification commande livrée ────────────────────────────────────────

    /**
     * Notifie le client et le vendeur quand une commande est livrée.
     */
    public void notifyOrderDelivered(CustomerOrder order) {
        // Notifier le client
        try {
            if (order.getCustomerPhone() != null && !order.getCustomerPhone().isBlank()) {
                String clientMsg = "✅ Livraison terminée !\n\n"
                        + "Bonjour " + order.getCustomerName() + ",\n"
                        + "Votre commande " + shopName + " N° " + order.getTrackingNumber()
                        + " a été livrée avec succès !\n\n"
                        + "Merci pour votre confiance 🙏\n"
                        + "N'hésitez pas à laisser un avis sur les produits achetés.";
                metaWhatsApp.sendText(order.getCustomerPhone(), clientMsg);
            }
        } catch (Exception e) {
            log.warn("⚠️ WhatsApp client (livré) échoué : {}", e.getMessage());
        }

        // Notifier le vendeur PRO/PREMIUM
        VendorUser vendor = order.getVendor();
        if (vendor != null && vendor.canManageOrders()
                && vendor.getPhone() != null && !vendor.getPhone().isBlank()) {
            try {
                String vendorMsg = "✅ Commande livrée !\n\n"
                        + "📦 N° : " + order.getTrackingNumber() + "\n"
                        + "👤 Client : " + order.getCustomerName() + "\n"
                        + "💰 Montant : " + order.getTotalAmountCfa() + " CFA\n\n"
                        + "La commande a été livrée avec succès. 🎉";
                metaWhatsApp.sendText(vendor.getPhone(), vendorMsg);
            } catch (Exception e) {
                log.warn("⚠️ WhatsApp vendeur (livré) échoué : {}", e.getMessage());
            }
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** Notifie le client que sa commande est confirmée */
    private void notifyClientOrderConfirmed(CustomerOrder order, String appBaseUrl) {
        try {
            if (order.getCustomerPhone() != null && !order.getCustomerPhone().isBlank()) {
                String trackUrl = appBaseUrl + "/tracking?trackingNumber=" + order.getTrackingNumber();
                String msg = "✅ Bonjour " + order.getCustomerName() + " !\n\n"
                        + "Votre commande " + shopName + " N° " + order.getTrackingNumber()
                        + " a été confirmée et est en cours de préparation. 📦\n\n"
                        + "🔍 Suivre votre commande : " + trackUrl + "\n\n"
                        + "Merci pour votre commande ! 🙏";
                metaWhatsApp.sendText(order.getCustomerPhone(), msg);
                log.info("✅ WhatsApp auto client : commande {} confirmée", order.getTrackingNumber());
            }
        } catch (Exception e) {
            log.warn("⚠️ WhatsApp client (confirmé) échoué : {}", e.getMessage());
        }
    }

    private String deliveryLabel(CustomerOrder order) {
        return isHomeDelivery(order) ? "Livraison domicile" : "Retrait boutique";
    }

    private boolean isHomeDelivery(CustomerOrder order) {
        return order.getDeliveryOption() != null
                && order.getDeliveryOption().name().equals("HOME");
    }

    private String waLink(String phone, String message) {
        if (phone == null || phone.isBlank()) return "#";
        String cleaned = phone.replaceAll("[^0-9]", "");
        return "https://wa.me/" + cleaned + "?text=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
    }

    /**
     * Vérifie si un client a atteint un seuil de fidélité chez un vendeur
     * et notifie le vendeur via WhatsApp.
     */
    private void checkAndNotifyLoyalty(CustomerOrder order) {
        try {
            Map<VendorUser, Long> itemsByVendor = new LinkedHashMap<>();
            for (var line : order.getLines()) {
                if (line.getProduct() != null && line.getProduct().getVendor() != null) {
                    VendorUser vendor = line.getProduct().getVendor();
                    itemsByVendor.merge(vendor, (long) line.getQuantity(), Long::sum);
                }
            }

            for (Map.Entry<VendorUser, Long> entry : itemsByVendor.entrySet()) {
                VendorUser vendor = entry.getKey();
                if (vendor.getPhone() == null || vendor.getPhone().isBlank()) continue;

                long totalBefore = orderRepository.countItemsByCustomerPhoneAndVendor(
                        order.getCustomerPhone(), vendor);
                long totalAfter = totalBefore + entry.getValue();

                String milestone = null;
                if (totalBefore < LOYALTY_THRESHOLD_1 && totalAfter >= LOYALTY_THRESHOLD_1) {
                    milestone = LOYALTY_THRESHOLD_1 + " articles";
                } else if (totalBefore < LOYALTY_THRESHOLD_2 && totalAfter >= LOYALTY_THRESHOLD_2) {
                    milestone = LOYALTY_THRESHOLD_2 + " articles";
                }

                if (milestone != null) {
                    String notif = "🌟 Client fidèle détecté !\n\n"
                            + "👤 " + order.getCustomerName() + " (" + order.getCustomerPhone() + ")\n"
                            + "a commandé " + milestone + " dans votre boutique !\n\n"
                            + "💡 Pensez à lui offrir une réduction ou une carte de fidélité pour le remercier.\n"
                            + "→ Créez une carte depuis votre espace vendeur : /vendor/loyalty";
                    metaWhatsApp.sendText(vendor.getPhone(), notif);
                    log.info("🌟 Notif fidélité envoyée au vendeur {} pour client {}",
                            vendor.getDisplayName(), order.getCustomerPhone());
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Vérification fidélité échouée (commande sauvegardée quand même): {}", e.getMessage());
        }
    }
}
