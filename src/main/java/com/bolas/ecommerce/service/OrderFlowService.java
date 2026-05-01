package com.bolas.ecommerce.service;

import com.bolas.ecommerce.model.CustomerOrder;
import com.bolas.ecommerce.model.OrderStatus;
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

/**
 * Gère le flow de validation des commandes.
 *
 * Logique simplifiée :
 *  - Le vendeur confirme ET prépare en une seule action : PENDING → READY
 *  - Le vendeur assigne un livreur approuvé puis envoie en livraison : READY → IN_DELIVERY
 *  - Le vendeur marque livrée : IN_DELIVERY → DELIVERED
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

    // ─── Admin confirme une commande PENDING → READY (compatibilité) ──────────

    /**
     * Admin valide la commande PENDING → READY (flow admin simplifié).
     * Retourne le lien WhatsApp pour fallback (wa.me).
     */
    @Transactional
    public String confirmOrder(CustomerOrder order, String vendorPhone, String appBaseUrl) {
        log.info("confirmOrder (admin): id={} status={}", order.getId(), order.getStatus());
        try {
            order.setStatus(OrderStatus.READY);
            orderRepository.save(order);
            auditLogService.orderStatusChanged(order.getId(), order.getTrackingNumber(), "READY (admin)");
        } catch (Exception e) {
            log.error("confirmOrder FAILED: {}", e.getMessage(), e);
            throw e;
        }

        // Notification WhatsApp Meta automatique au vendeur
        VendorUser vendor = order.getVendor();
        if (vendor != null && vendor.getPhone() != null && !vendor.getPhone().isBlank()) {
            try {
                String msg = "🛍️ Commande validée par l'admin " + shopName + " !\n\n"
                        + "📦 N° : " + order.getTrackingNumber() + "\n"
                        + "👤 Client : " + order.getCustomerName() + "\n"
                        + "📞 Tél : " + order.getCustomerPhone() + "\n"
                        + "📍 Option : " + deliveryLabel(order) + "\n"
                        + "💰 Total : " + order.getTotalAmountCfa() + " CFA\n\n"
                        + "→ Assignez un livreur et expédiez : " + appBaseUrl + "/vendor/orders";
                metaWhatsApp.sendText(vendor.getPhone(), msg);
            } catch (Exception e) {
                log.warn("⚠️ WhatsApp vendeur échoué : {}", e.getMessage());
            }
        }

        notifyClientOrderConfirmed(order, appBaseUrl);
        checkAndNotifyLoyalty(order);

        String msg = "🛍️ Commande prête à expédier !\n\n"
                + "N° : " + order.getTrackingNumber() + "\n"
                + "Client : " + order.getCustomerName() + "\n"
                + "Option : " + deliveryLabel(order) + "\n"
                + "Montant : " + order.getTotalAmountCfa() + " CFA\n\n"
                + "→ Assignez un livreur : " + appBaseUrl + "/vendor/orders";
        return waLink(vendorPhone, msg);
    }

    // ─── Vendeur confirme + prépare PENDING → READY (1 seule action) ─────────

    /**
     * Vendeur confirme ET prépare la commande en une seule action : PENDING → READY.
     * Notifie le client que sa commande est en préparation.
     */
    @Transactional
    public void vendorConfirmOrder(CustomerOrder order, VendorUser vendor, String appBaseUrl) {
        order.setStatus(OrderStatus.READY);
        orderRepository.save(order);
        auditLogService.orderStatusChanged(order.getId(), order.getTrackingNumber(), "READY (vendeur)");

        // Notifier l'admin
        try {
            String adminMsg = "✅ Commande prête par le vendeur !\n\n"
                    + "🏪 Vendeur : " + vendor.getDisplayName() + "\n"
                    + "📦 N° : " + order.getTrackingNumber() + "\n"
                    + "👤 Client : " + order.getCustomerName() + "\n"
                    + "💰 Total : " + order.getTotalAmountCfa() + " CFA\n\n"
                    + "→ Le vendeur gère la livraison.";
            metaWhatsApp.sendText(shopWhatsapp, adminMsg);
        } catch (Exception e) {
            log.warn("⚠️ WhatsApp admin échoué : {}", e.getMessage());
        }

        notifyClientOrderConfirmed(order, appBaseUrl);
        checkAndNotifyLoyalty(order);
        log.info("✅ Vendeur {} a confirmé+préparé commande {}", vendor.getDisplayName(), order.getTrackingNumber());
    }

    // ─── Admin OU Vendeur envoie en livraison READY → IN_DELIVERY ─────────────

    /**
     * Passe la commande en livraison (READY → IN_DELIVERY).
     */
    @Transactional
    public String notifyClientReady(CustomerOrder order, String appBaseUrl) {
        order.setStatus(OrderStatus.IN_DELIVERY);
        orderRepository.save(order);
        auditLogService.orderStatusChanged(order.getId(), order.getTrackingNumber(), "IN_DELIVERY");

        String trackUrl = appBaseUrl + "/tracking?trackingNumber=" + order.getTrackingNumber();

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
            }
        } catch (Exception e) {
            log.warn("⚠️ WhatsApp client (livraison) échoué : {}", e.getMessage());
        }

        try {
            VendorUser vendor = order.getVendor();
            String adminMsg = "🚚 Commande en livraison\n\n"
                    + "📦 N° : " + order.getTrackingNumber() + "\n"
                    + "🏪 Vendeur : " + (vendor != null ? vendor.getDisplayName() : "—") + "\n"
                    + "👤 Client : " + order.getCustomerName() + "\n"
                    + (order.getAssignedCourierName() != null ? "🚴 Livreur : " + order.getAssignedCourierName() + "\n" : "");
            metaWhatsApp.sendText(shopWhatsapp, adminMsg);
        } catch (Exception e) {
            log.warn("⚠️ WhatsApp admin (livraison) échoué : {}", e.getMessage());
        }

        String msg = "Bonjour " + order.getCustomerName() + " 👋\n\n"
                + "Votre commande " + shopName + " est prête"
                + (isHomeDelivery(order) ? " et en cours de livraison 🚚" : " pour le retrait 🏪") + " !\n\n"
                + "📦 N° de suivi : " + order.getTrackingNumber() + "\n"
                + "🔍 Suivre votre commande : " + trackUrl;
        return waLink(order.getCustomerPhone(), msg);
    }

    // ─── Vendeur envoie en livraison READY → IN_DELIVERY ─────────────────────

    @Transactional
    public void vendorStartDelivery(CustomerOrder order, VendorUser vendor, String appBaseUrl) {
        notifyClientReady(order, appBaseUrl);
        log.info("✅ Vendeur {} a lancé la livraison de commande {}", vendor.getDisplayName(), order.getTrackingNumber());
    }

    // ─── Vendeur marque IN_DELIVERY → DELIVERED ──────────────────────────────

    @Transactional
    public void vendorMarkDelivered(CustomerOrder order, VendorUser vendor) {
        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);
        auditLogService.orderStatusChanged(order.getId(), order.getTrackingNumber(), "DELIVERED (vendeur)");
        notifyOrderDelivered(order);
        log.info("✅ Vendeur {} a marqué commande {} comme livrée", vendor.getDisplayName(), order.getTrackingNumber());
    }

    // ─── Notification commande annulée ───────────────────────────────────────

    public void notifyVendorOrderCancelled(CustomerOrder order) {
        VendorUser vendor = order.getVendor();
        if (vendor != null && vendor.getPhone() != null && !vendor.getPhone().isBlank()) {
            try {
                String msg = "❌ Commande annulée\n\n"
                        + "📦 N° : " + order.getTrackingNumber() + "\n"
                        + "👤 Client : " + order.getCustomerName() + "\n"
                        + "💰 Montant : " + order.getTotalAmountCfa() + " CFA\n\n"
                        + "Cette commande a été annulée par l'admin " + shopName + ".";
                metaWhatsApp.sendText(vendor.getPhone(), msg);
            } catch (Exception e) {
                log.warn("⚠️ WhatsApp vendeur (annulation) échoué : {}", e.getMessage());
            }
        }
    }

    // ─── Notification commande livrée ────────────────────────────────────────

    public void notifyOrderDelivered(CustomerOrder order) {
        try {
            if (order.getCustomerPhone() != null && !order.getCustomerPhone().isBlank()) {
                String clientMsg = "✅ Livraison terminée !\n\n"
                        + "Bonjour " + order.getCustomerName() + ",\n"
                        + "Votre commande " + shopName + " N° " + order.getTrackingNumber()
                        + " a été livrée avec succès !\n\n"
                        + "Merci pour votre confiance 🙏";
                metaWhatsApp.sendText(order.getCustomerPhone(), clientMsg);
            }
        } catch (Exception e) {
            log.warn("⚠️ WhatsApp client (livré) échoué : {}", e.getMessage());
        }

        VendorUser vendor = order.getVendor();
        if (vendor != null && vendor.getPhone() != null && !vendor.getPhone().isBlank()) {
            try {
                String vendorMsg = "✅ Commande livrée !\n\n"
                        + "📦 N° : " + order.getTrackingNumber() + "\n"
                        + "👤 Client : " + order.getCustomerName() + "\n"
                        + "💰 Montant : " + order.getTotalAmountCfa() + " CFA 🎉";
                metaWhatsApp.sendText(vendor.getPhone(), vendorMsg);
            } catch (Exception e) {
                log.warn("⚠️ WhatsApp vendeur (livré) échoué : {}", e.getMessage());
            }
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void notifyClientOrderConfirmed(CustomerOrder order, String appBaseUrl) {
        try {
            if (order.getCustomerPhone() != null && !order.getCustomerPhone().isBlank()) {
                String trackUrl = appBaseUrl + "/tracking?trackingNumber=" + order.getTrackingNumber();
                String msg = "✅ Bonjour " + order.getCustomerName() + " !\n\n"
                        + "Votre commande " + shopName + " N° " + order.getTrackingNumber()
                        + " est confirmée et en cours de préparation. 📦\n\n"
                        + "🔍 Suivre votre commande : " + trackUrl + "\n\n"
                        + "Merci pour votre commande ! 🙏";
                metaWhatsApp.sendText(order.getCustomerPhone(), msg);
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

    private void checkAndNotifyLoyalty(CustomerOrder order) {
        try {
            java.util.Map<VendorUser, Long> itemsByVendor = new LinkedHashMap<>();
            for (var line : order.getLines()) {
                if (line.getProduct() != null && line.getProduct().getVendor() != null) {
                    VendorUser vendor = line.getProduct().getVendor();
                    itemsByVendor.merge(vendor, (long) line.getQuantity(), Long::sum);
                }
            }

            for (java.util.Map.Entry<VendorUser, Long> entry : itemsByVendor.entrySet()) {
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
                            + "💡 Pensez à lui offrir une réduction ou une carte de fidélité.";
                    metaWhatsApp.sendText(vendor.getPhone(), notif);
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Vérification fidélité échouée : {}", e.getMessage());
        }
    }
}
