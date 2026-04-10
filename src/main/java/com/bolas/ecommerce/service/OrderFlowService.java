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
import java.util.Map;

/**
 * Gère le flow de validation des commandes et génère les liens WhatsApp
 * de notification à chaque étape.
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

    @Value("${bolas.shop.name:Bola's}")
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

    /**
     * Admin valide la commande PENDING → CONFIRMED.
     * Retourne le lien WhatsApp pour notifier le vendeur.
     */
    @Transactional
    public String confirmOrder(CustomerOrder order, String vendorPhone, String appBaseUrl) {
        log.info("confirmOrder: id={} status={}", order.getId(), order.getStatus());
        try {
            order.setStatus(OrderStatus.CONFIRMED);
            log.info("confirmOrder: saving with status CONFIRMED");
            orderRepository.save(order);
            log.info("confirmOrder: saved OK");
            auditLogService.orderStatusChanged(order.getId(), order.getTrackingNumber(), "CONFIRMED");
        } catch (Exception e) {
            log.error("confirmOrder FAILED: {}", e.getMessage(), e);
            throw e;
        }

        // Message WhatsApp pour le vendeur
        String msg = "🛍️ Nouvelle commande à préparer !\n\n"
                + "N° : " + order.getTrackingNumber() + "\n"
                + "Client : " + order.getCustomerName() + "\n"
                + "Tél : " + order.getCustomerPhone() + "\n"
                + "Option : " + (order.getDeliveryOption().name().equals("HOME") ? "Livraison domicile" : "Retrait boutique") + "\n"
                + "Montant : " + order.getTotalAmountCfa() + " CFA\n\n"
                + "Connectez-vous pour préparer : " + appBaseUrl + "/vendor/orders";

        // Vérification fidélité : notifier le vendeur si seuil atteint
        checkAndNotifyLoyalty(order);

        return waLink(vendorPhone, msg);
    }

    /**
     * Vendeur valide la préparation CONFIRMED → READY.
     * Retourne le lien WhatsApp pour notifier l'admin.
     */
    @Transactional
    public String markReady(CustomerOrder order, String appBaseUrl) {
        order.setStatus(OrderStatus.READY);
        orderRepository.save(order);
        auditLogService.orderStatusChanged(order.getId(), order.getTrackingNumber(), "READY");

        String msg = "✅ Commande prête !\n\n"
                + "N° : " + order.getTrackingNumber() + "\n"
                + "Client : " + order.getCustomerName() + "\n"
                + "Option : " + (order.getDeliveryOption().name().equals("HOME") ? "Livraison domicile" : "Retrait boutique") + "\n\n"
                + "Voir dans l'admin : " + appBaseUrl + "/admin/orders";

        return waLink(shopWhatsapp, msg);
    }

    /**
     * Admin informe le client que sa commande est prête / en livraison.
     * Retourne le lien WhatsApp pour notifier le client.
     */
    @Transactional
    public String notifyClientReady(CustomerOrder order, String appBaseUrl) {
        order.setStatus(OrderStatus.IN_DELIVERY);
        orderRepository.save(order);
        auditLogService.orderStatusChanged(order.getId(), order.getTrackingNumber(), "IN_DELIVERY");

        String trackUrl = appBaseUrl + "/tracking?trackingNumber=" + order.getTrackingNumber();
        String msg = "Bonjour " + order.getCustomerName() + " 👋\n\n"
                + "Votre commande " + shopName + " est prête"
                + (order.getDeliveryOption().name().equals("HOME") ? " et en cours de livraison 🚚" : " pour le retrait 🏪") + " !\n\n"
                + "📦 N° de suivi : " + order.getTrackingNumber() + "\n"
                + "🔍 Suivre votre commande : " + trackUrl;

        return waLink(order.getCustomerPhone(), msg);
    }

    private String waLink(String phone, String message) {
        // Nettoie le numéro (enlève espaces, tirets, +)
        String cleaned = phone.replaceAll("[^0-9]", "");
        return "https://wa.me/" + cleaned + "?text=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
    }

    /**
     * Vérifie si un client a atteint un seuil de fidélité chez un vendeur
     * et notifie le vendeur via WhatsApp.
     */
    private void checkAndNotifyLoyalty(CustomerOrder order) {
        try {
            // Regrouper les articles par vendeur dans cette commande
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

                // Total cumulé de cet acheteur chez ce vendeur (commandes passées + actuelle)
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
