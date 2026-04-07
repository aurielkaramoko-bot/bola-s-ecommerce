package com.bolas.ecommerce.service;

import com.bolas.ecommerce.model.CustomerOrder;
import com.bolas.ecommerce.model.OrderStatus;
import com.bolas.ecommerce.repository.CustomerOrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Gère le flow de validation des commandes et génère les liens WhatsApp
 * de notification à chaque étape.
 */
@Service
public class OrderFlowService {

    private final CustomerOrderRepository orderRepository;
    private final AuditLogService auditLogService;

    @Value("${whatsapp.number}")
    private String shopWhatsapp;

    @Value("${bolas.shop.name:Bola's}")
    private String shopName;

    public OrderFlowService(CustomerOrderRepository orderRepository,
                            AuditLogService auditLogService) {
        this.orderRepository = orderRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Admin valide la commande PENDING → CONFIRMED.
     * Retourne le lien WhatsApp pour notifier le vendeur.
     */
    @Transactional
    public String confirmOrder(CustomerOrder order, String vendorPhone, String appBaseUrl) {
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        auditLogService.orderStatusChanged(order.getId(), order.getTrackingNumber(), "CONFIRMED");

        // Message WhatsApp pour le vendeur
        String msg = "🛍️ Nouvelle commande à préparer !\n\n"
                + "N° : " + order.getTrackingNumber() + "\n"
                + "Client : " + order.getCustomerName() + "\n"
                + "Tél : " + order.getCustomerPhone() + "\n"
                + "Option : " + (order.getDeliveryOption().name().equals("HOME") ? "Livraison domicile" : "Retrait boutique") + "\n"
                + "Montant : " + order.getTotalAmountCfa() + " CFA\n\n"
                + "Connectez-vous pour préparer : " + appBaseUrl + "/vendor/orders";

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
}
