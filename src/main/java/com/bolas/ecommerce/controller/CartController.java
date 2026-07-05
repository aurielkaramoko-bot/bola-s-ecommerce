package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.model.Customer;
import com.bolas.ecommerce.model.CustomerOrder;
import com.bolas.ecommerce.model.DeliveryOption;
import com.bolas.ecommerce.model.NotificationDestinataire;
import com.bolas.ecommerce.model.NotificationType;
import com.bolas.ecommerce.model.OrderLine;
import com.bolas.ecommerce.model.VendorPlan;
import com.bolas.ecommerce.model.VendorUser;
import com.bolas.ecommerce.repository.CountryRepository;
import com.bolas.ecommerce.repository.CustomerOrderRepository;
import com.bolas.ecommerce.service.CommissionService;
import com.bolas.ecommerce.service.MetaWhatsAppService;
import com.bolas.ecommerce.service.CartService;
import com.bolas.ecommerce.service.CustomerService;
import com.bolas.ecommerce.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;
    private final CustomerOrderRepository orderRepository;
    private final CustomerService customerService;
    private final CountryRepository countryRepository;
    private final CommissionService commissionService;
    private final MetaWhatsAppService metaWhatsApp;
    private final NotificationService notificationService;
    private final com.bolas.ecommerce.service.InteractionTrackingService interactionTrackingService;
    private final String whatsappNumber;

    @Value("${app.base-url:https://bola-marketplace.onrender.com}")
    private String appBaseUrl;

    public CartController(CartService cartService,
                          CustomerOrderRepository orderRepository,
                          CustomerService customerService,
                          CountryRepository countryRepository,
                          CommissionService commissionService,
                          MetaWhatsAppService metaWhatsApp,
                          NotificationService notificationService,
                          com.bolas.ecommerce.service.InteractionTrackingService interactionTrackingService,
                          @Value("${whatsapp.number}") String whatsappNumber) {
        this.cartService = cartService;
        this.orderRepository = orderRepository;
        this.customerService = customerService;
        this.countryRepository = countryRepository;
        this.commissionService = commissionService;
        this.metaWhatsApp = metaWhatsApp;
        this.notificationService = notificationService;
        this.interactionTrackingService = interactionTrackingService;
        this.whatsappNumber = whatsappNumber;
    }

    @GetMapping("/cart")
    @Transactional(readOnly = true)
    public String cart(Model model, HttpSession session) {
        model.addAttribute("pageTitle", "Panier — Bola's");
        model.addAttribute("cartLines", cartService.lines(session));
        model.addAttribute("cartTotalCfa", cartService.totalAmountCfa(session));
        Customer customer = (Customer) session.getAttribute("BOLAS_CUSTOMER");
        model.addAttribute("connectedCustomer", customer);
        try {
            model.addAttribute("countries", countryRepository.findByActiveTrueOrderByNameAsc());
        } catch (Exception e) {
            model.addAttribute("countries", java.util.List.of());
        }
        return "cart";
    }

    @PostMapping("/cart/add")
    public String add(@RequestParam long productId,
                      @RequestParam(defaultValue = "1") int qty,
                      @RequestParam(required = false) String returnTo,
                      HttpServletRequest request,
                      HttpSession session,
                      RedirectAttributes ra) {
        cartService.add(session, productId, Math.min(Math.max(qty, 1), 99));
        ra.addFlashAttribute("flashOk", "Ajouté au panier.");

        // ── Tracking IA : ajout panier ────────────────────────────────────
        try {
            Customer customer = (Customer) session.getAttribute("BOLAS_CUSTOMER");
            if (customer != null) {
                interactionTrackingService.trackAddToCart(customer.getId(), productId);
            }
        } catch (Exception e) {
            log.debug("Tracking ADD_TO_CART ignoré: {}", e.getMessage());
        }
        if (returnTo != null && returnTo.startsWith("/")) {
            return "redirect:" + returnTo;
        }
        String referer = request.getHeader("Referer");
        if (referer != null) {
            int idx = referer.indexOf(request.getContextPath());
            if (idx >= 0) {
                String path = referer.substring(idx + request.getContextPath().length());
                if (path.startsWith("/")) {
                    return "redirect:" + path;
                }
            }
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/update")
    public String update(@RequestParam long productId,
                         @RequestParam int qty,
                         HttpSession session) {
        cartService.setQuantity(session, productId, Math.min(Math.max(qty, 0), 99));
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String remove(@RequestParam long productId, HttpSession session) {
        cartService.remove(session, productId);
        return "redirect:/cart";
    }

    @PostMapping("/cart/clear")
    public String clear(HttpSession session) {
        cartService.clear(session);
        return "redirect:/cart";
    }

    @PostMapping("/cart/checkout")
    @Transactional
    public String checkout(@RequestParam String customerName,
                           @RequestParam String customerPhone,
                           @RequestParam(required = false, defaultValue = "") String customerAddress,
                           @RequestParam(defaultValue = "HOME") String deliveryOption,
                           @RequestParam(required = false, defaultValue = "TG") String country,
                           @RequestParam(required = false) Double clientLatitude,
                           @RequestParam(required = false) Double clientLongitude,
                           HttpSession session,
                           RedirectAttributes ra) {

        String cleanedPhone = null;
        
        try {
            log.info("🛒 Début checkout pour client {}", customerName);
            
            // Forcer la connexion si pas de compte client
            if (session.getAttribute("BOLAS_CUSTOMER") == null) {
                log.warn("⚠️ Tentative checkout sans connexion");
                ra.addFlashAttribute("flashError", "Veuillez vous connecter ou créer un compte pour commander.");
                return "redirect:/customer/login";
            }
            
            // Validation WhatsApp
            if (whatsappNumber == null || whatsappNumber.isBlank()) {
                log.error("❌ Configuration WhatsApp manquante!");
                ra.addFlashAttribute("flashError", "Configuration WhatsApp manquante. Contactez l'administrateur.");
                return "redirect:/cart";
            }
            cleanedPhone = whatsappNumber.replaceAll("[^0-9]", "");
            if (cleanedPhone.length() < 9) {
                log.error("❌ Numéro WhatsApp invalide: {}", whatsappNumber);
                ra.addFlashAttribute("flashError", "Numéro WhatsApp invalide. Contactez l'administrateur.");
                return "redirect:/cart";
            }
        } catch (Exception e) {
            log.error("❌ Erreur validation checkout", e);
            ra.addFlashAttribute("flashError", "Erreur lors de la validation: " + e.getMessage());
            return "redirect:/cart";
        }

        var lines = cartService.lines(session);
        if (lines.isEmpty()) {
            log.warn("⚠️ Panier vide pour {}", customerName);
            ra.addFlashAttribute("flashError", "Votre panier est vide.");
            return "redirect:/cart";
        }
        log.info("   → Panier: {} articles", lines.size());

        // ── Grouper les lignes par vendeur ──────────────────────────────────────
        // Clé : ID du vendeur (null → produit BOLA sans vendeur)
        Map<Long, List<CartService.CartLine>> linesByVendor = lines.stream()
                .collect(Collectors.groupingBy(
                        l -> l.product().getVendor() != null ? l.product().getVendor().getId() : 0L,
                        LinkedHashMap::new, Collectors.toList()));

        Customer customer = (Customer) session.getAttribute("BOLAS_CUSTOMER");
        List<CustomerOrder> createdOrders = new ArrayList<>();

        // Taxe douanière (commune à toutes les commandes)
        var countryOpt = countryRepository.findByCode(country.toUpperCase());
        int customsTaxPercent = 0;
        if (countryOpt.isPresent() && countryOpt.get().getCustomsTaxPercent() > 0) {
            customsTaxPercent = countryOpt.get().getCustomsTaxPercent();
        }

        // ── Créer une commande par vendeur ─────────────────────────────────────
        for (var entry : linesByVendor.entrySet()) {
            List<CartService.CartLine> vendorLines = entry.getValue();

            // Identifier le vendeur de ce groupe
            VendorUser vendor = vendorLines.stream()
                    .map(l -> l.product().getVendor())
                    .filter(v -> v != null)
                    .findFirst()
                    .orElse(null);

            // Sous-total de ce groupe
            long subtotal = vendorLines.stream()
                    .mapToLong(CartService.CartLine::lineTotalCfa).sum();
            long customsTax = subtotal * customsTaxPercent / 100;

            CustomerOrder order = new CustomerOrder();

            // Tracking number
            String trackingNumber;
            if (customer != null) {
                trackingNumber = customerService.generateTrackingNumber(customer, country);
            } else {
                trackingNumber = "BOL-" + country.toUpperCase() + "-"
                        + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            }
            order.setTrackingNumber(trackingNumber);
            order.setCountry(country.toUpperCase());
            order.setCustomerName(customerName.trim());
            order.setCustomerPhone(customerPhone.trim());
            order.setCustomerAddress(customerAddress.trim());
            order.setDeliveryOption("PICKUP".equals(deliveryOption) ? DeliveryOption.PICKUP : DeliveryOption.HOME);
            order.setTotalAmountCfa(subtotal + customsTax);
            order.setDeliveryFeeCfa(0L);

            // Commission BOLA selon le plan du vendeur
            VendorPlan vendorPlan = vendor != null ? vendor.getPlan() : null;
            int commissionPct = commissionService.rateFor(vendorPlan);
            long commissionAmt = commissionService.compute(subtotal, vendorPlan);
            order.setCommissionPercent(commissionPct);
            order.setCommissionCfa(commissionAmt);
            order.setVendor(vendor);
            if (clientLatitude != null) order.setClientLatitude(clientLatitude);
            if (clientLongitude != null) order.setClientLongitude(clientLongitude);

            for (var line : vendorLines) {
                OrderLine ol = new OrderLine();
                ol.setProduct(line.product());
                ol.setQuantity(line.quantity());
                ol.setUnitPriceCfa(line.product().getEffectivePriceCfa());
                order.addLine(ol);
            }
            orderRepository.save(order);
            createdOrders.add(order);
            log.info("   → Commande sauvegardée: {} (vendeur: {})", order.getTrackingNumber(),
                    vendor != null ? vendor.getDisplayName() : "BOLA direct");

            // ── Tracking IA : achat confirmé ──────────────────────────────
            if (customer != null) {
                for (var line : vendorLines) {
                    try {
                        interactionTrackingService.trackPurchase(customer.getId(), line.product().getId());
                    } catch (Exception e) {
                        log.debug("Tracking PURCHASE ignoré: {}", e.getMessage());
                    }
                }
            }

            // Notification in-app au vendeur PRO/PREMIUM
            if (vendor != null) {
                notificationService.envoyer(
                    vendor.getId(), NotificationDestinataire.VENDEUR,
                    NotificationType.COMMANDE,
                    "🛒 Nouvelle commande !",
                    "N° " + order.getTrackingNumber() + " — " + customerName.trim()
                        + " — " + order.getTotalAmountCfa() + " CFA",
                    "/vendor/orders"
                );
            }

            // Notifier le vendeur PRO/PREMIUM directement via WhatsApp
            if (vendor != null && vendor.getPhone() != null
                    && (vendor.getPlan() == VendorPlan.PRO
                        || vendor.getPlan() == VendorPlan.PRO_LOCAL
                        || vendor.getPlan() == VendorPlan.PREMIUM)) {
                try {
                    StringBuilder vendorMsg = new StringBuilder();
                    vendorMsg.append("🛍️ Nouvelle commande pour votre boutique !\n\n");
                    vendorMsg.append("📦 N° : ").append(order.getTrackingNumber()).append("\n");
                    vendorMsg.append("👤 Client : ").append(customerName.trim()).append("\n");
                    vendorMsg.append("📞 Tél : ").append(customerPhone.trim()).append("\n");
                    vendorMsg.append("📍 Option : ").append("PICKUP".equals(deliveryOption) ? "Retrait boutique" : "Livraison domicile").append("\n");
                    if (clientLatitude != null && clientLongitude != null) {
                        vendorMsg.append("🗺️ Position GPS : https://maps.google.com/?q=")
                                 .append(clientLatitude).append(",").append(clientLongitude).append("\n");
                    } else if (!customerAddress.isBlank()) {
                        vendorMsg.append("🏠 Adresse : ").append(customerAddress.trim()).append("\n");
                    }
                    vendorMsg.append("💰 Total : ").append(order.getTotalAmountCfa()).append(" CFA\n\n");
                    vendorMsg.append("Produits commandés :\n");
                    for (var line : vendorLines) {
                        vendorMsg.append("• ").append(line.product().getName())
                                 .append(" x").append(line.quantity()).append("\n");
                    }
                    vendorMsg.append("\n→ Préparez la commande : ").append(appBaseUrl).append("/vendor/orders");
                    metaWhatsApp.sendText(vendor.getPhone(), vendorMsg.toString());
                    log.info("   → Vendeur {} notifié", vendor.getDisplayName());
                } catch (Exception e) {
                    log.warn("Notification WhatsApp vendeur échouée : {}", e.getMessage());
                }
            }
        }

        cartService.clear(session);
        log.info("   → Panier vidé");

        // Notifier l'admin (résumé de toutes les commandes)
        log.info("   → Envoi notification admin...");
        try {
            StringBuilder adminMsg = new StringBuilder();
            adminMsg.append("\uD83D\uDED2 Nouvelle(s) commande(s) sur BOLA !\n\n");
            adminMsg.append("\uD83D\uDC64 Client : ").append(customerName.trim()).append("\n");
            adminMsg.append("\uD83D\uDCDE Tél : ").append(customerPhone.trim()).append("\n");
            adminMsg.append("\uD83C\uDF0D Pays : ").append(country.toUpperCase()).append("\n\n");
            for (CustomerOrder o : createdOrders) {
                adminMsg.append("\uD83D\uDCE6 N° : ").append(o.getTrackingNumber());
                if (o.getVendor() != null) {
                    adminMsg.append(" (").append(o.getVendor().getDisplayName()).append(")");
                }
                adminMsg.append(" — ").append(o.getTotalAmountCfa()).append(" CFA");
                if (o.getCommissionCfa() > 0) {
                    adminMsg.append(" [commission ").append(o.getCommissionPercent()).append("% = ")
                            .append(o.getCommissionCfa()).append(" CFA]");
                }
                adminMsg.append("\n");
            }
            adminMsg.append("\n\u2192 Voir dans l'admin BOLA");
            metaWhatsApp.sendText(whatsappNumber, adminMsg.toString());
        } catch (Exception e) {
            log.warn("Notification WhatsApp admin échouée (commandes sauvegardées quand même): {}", e.getMessage());
        }

        // Redirection WhatsApp vers le principal vendeur ou l'admin
        // On utilise la première commande comme référence pour le message WhatsApp
        CustomerOrder firstOrder = createdOrders.get(0);
        VendorUser mainVendor = firstOrder.getVendor();
        String targetPhone = cleanedPhone; // admin par défaut
        String targetName = "Bola's";
        if (mainVendor != null && mainVendor.getPhone() != null && !mainVendor.getPhone().isBlank()) {
            targetPhone = mainVendor.getPhone().replaceAll("[^0-9]", "");
            targetName = mainVendor.getDisplayName();
        }

        long grandTotal = createdOrders.stream().mapToLong(CustomerOrder::getTotalAmountCfa).sum();

        StringBuilder msg = new StringBuilder();
        msg.append("Bonjour ").append(targetName).append(" 👋\nJe souhaite commander :\n\n");
        for (var line : lines) {
            msg.append("• ").append(line.product().getName())
               .append(" x").append(line.quantity())
               .append(" = ").append(line.lineTotalCfa()).append(" CFA\n");
        }
        msg.append("\nTotal : ").append(grandTotal).append(" CFA");
        msg.append("\nPays : ").append(country.toUpperCase());
        msg.append("\nOption : ").append("PICKUP".equals(deliveryOption) ? "Retrait en boutique" : "Livraison à domicile");
        if (clientLatitude != null && clientLongitude != null) {
            msg.append("\n🗺️ Position GPS : https://maps.google.com/?q=")
               .append(clientLatitude).append(",").append(clientLongitude);
        } else if (!customerAddress.isBlank()) {
            msg.append("\n🏠 Adresse : ").append(customerAddress.trim());
        }
        // Lister les numéros de suivi
        msg.append("\n\n📦 N° de suivi : ");
        msg.append(createdOrders.stream()
                .map(CustomerOrder::getTrackingNumber)
                .collect(Collectors.joining(", ")));

        String waUrl = "https://wa.me/" + targetPhone
                + "?text=" + URLEncoder.encode(msg.toString(), StandardCharsets.UTF_8);

        return "redirect:" + waUrl;
    }
}
