package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.model.Customer;
import com.bolas.ecommerce.model.CustomerOrder;
import com.bolas.ecommerce.model.DeliveryOption;
import com.bolas.ecommerce.model.OrderLine;
import com.bolas.ecommerce.model.VendorPlan;
import com.bolas.ecommerce.repository.CountryRepository;
import com.bolas.ecommerce.repository.CustomerOrderRepository;
import com.bolas.ecommerce.service.CommissionService;
import com.bolas.ecommerce.service.MetaWhatsAppService;
import com.bolas.ecommerce.service.CartService;
import com.bolas.ecommerce.service.CustomerService;
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
import java.util.UUID;

@Controller
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;
    private final CustomerOrderRepository orderRepository;
    private final CustomerService customerService;
    private final CountryRepository countryRepository;
    private final CommissionService commissionService;
    private final MetaWhatsAppService metaWhatsApp;
    private final String whatsappNumber;

    public CartController(CartService cartService,
                          CustomerOrderRepository orderRepository,
                          CustomerService customerService,
                          CountryRepository countryRepository,
                          CommissionService commissionService,
                          MetaWhatsAppService metaWhatsApp,
                          @Value("${whatsapp.number}") String whatsappNumber) {
        this.cartService = cartService;
        this.orderRepository = orderRepository;
        this.customerService = customerService;
        this.countryRepository = countryRepository;
        this.commissionService = commissionService;
        this.metaWhatsApp = metaWhatsApp;
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

        // Forcer la connexion si pas de compte client
        if (session.getAttribute("BOLAS_CUSTOMER") == null) {
            ra.addFlashAttribute("flashError", "Veuillez vous connecter ou créer un compte pour commander.");
            return "redirect:/customer/login";
        }

        var lines = cartService.lines(session);
        if (lines.isEmpty()) {
            ra.addFlashAttribute("flashError", "Votre panier est vide.");
            return "redirect:/cart";
        }

        // Créer la commande
        CustomerOrder order = new CustomerOrder();

        // Tracking number personnalisé si client connecté, sinon UUID classique
        Customer customer = (Customer) session.getAttribute("BOLAS_CUSTOMER");
        String trackingNumber;
        if (customer != null) {
            trackingNumber = customerService.generateTrackingNumber(customer, country);
        } else {
            trackingNumber = "BOL-" + country.toUpperCase() + "-" + UUID.randomUUID().toString().replace("-","").substring(0,8).toUpperCase();
        }
        order.setTrackingNumber(trackingNumber);
        order.setCountry(country.toUpperCase());
        order.setCustomerName(customerName.trim());
        order.setCustomerPhone(customerPhone.trim());
        order.setCustomerAddress(customerAddress.trim());
        order.setDeliveryOption("PICKUP".equals(deliveryOption) ? DeliveryOption.PICKUP : DeliveryOption.HOME);

        // Calcul taxe douanière selon le pays
        long subtotal = cartService.totalAmountCfa(session);
        long customsTax = 0L;
        var countryOpt = countryRepository.findByCode(country.toUpperCase());
        if (countryOpt.isPresent() && countryOpt.get().getCustomsTaxPercent() > 0) {
            customsTax = subtotal * countryOpt.get().getCustomsTaxPercent() / 100;
        }
        order.setTotalAmountCfa(subtotal + customsTax);
        order.setDeliveryFeeCfa(0L);

        // Calcul commission BOLA selon le plan du vendeur principal
        VendorPlan vendorPlan = lines.stream()
                .map(l -> l.product().getVendor())
                .filter(v -> v != null)
                .findFirst()
                .map(v -> v.getPlan())
                .orElse(null);
        int commissionPct = commissionService.rateFor(vendorPlan);
        long commissionAmt = commissionService.compute(subtotal, vendorPlan);
        order.setCommissionPercent(commissionPct);
        order.setCommissionCfa(commissionAmt);
        if (clientLatitude != null)  order.setClientLatitude(clientLatitude);
        if (clientLongitude != null) order.setClientLongitude(clientLongitude);

        for (var line : lines) {
            OrderLine ol = new OrderLine();
            ol.setProduct(line.product());
            ol.setQuantity(line.quantity());
            ol.setUnitPriceCfa(line.product().getEffectivePriceCfa());
            order.addLine(ol);
        }
        orderRepository.save(order);
        cartService.clear(session);

        // Notifier l'admin automatiquement via Meta WhatsApp
        try {
            StringBuilder adminMsg = new StringBuilder();
            adminMsg.append("\uD83D\uDED2 Nouvelle commande sur BOLA !\n\n");
            adminMsg.append("\uD83D\uDCE6 N\u00b0 : ").append(order.getTrackingNumber()).append("\n");
            adminMsg.append("\uD83D\uDC64 Client : ").append(customerName.trim()).append("\n");
            adminMsg.append("\uD83D\uDCDE T\u00e9l : ").append(customerPhone.trim()).append("\n");
            adminMsg.append("\uD83C\uDF0D Pays : ").append(country.toUpperCase()).append("\n");
            adminMsg.append("\uD83D\uDCB0 Total : ").append(order.getTotalAmountCfa()).append(" CFA\n");
            if (commissionAmt > 0) {
                adminMsg.append("\uD83D\uDCB5 Commission BOLA (").append(commissionPct).append("%) : ")
                        .append(commissionAmt).append(" CFA\n");
            }
            adminMsg.append("\n\u2192 Voir dans l'admin BOLA");
            metaWhatsApp.sendText(whatsappNumber, adminMsg.toString());
        } catch (Exception e) {
            log.warn("Notification WhatsApp admin \u00e9chou\u00e9e (commande sauvegard\u00e9e quand m\u00eame): {}", e.getMessage());
        }

        // Construire le message WhatsApp
        StringBuilder msg = new StringBuilder();
        msg.append("Bonjour Bola's \uD83D\uDC4B\nJe souhaite commander :\n\n");
        for (var line : lines) {
            msg.append("• ").append(line.product().getName())
               .append(" x").append(line.quantity())
               .append(" = ").append(line.lineTotalCfa()).append(" CFA\n");
        }
        msg.append("\nSous-total : ").append(subtotal).append(" CFA");
        if (customsTax > 0) {
            msg.append("\nTaxe douanière (").append(countryOpt.get().getCustomsTaxPercent()).append("%) : ").append(customsTax).append(" CFA");
        }
        msg.append("\nTotal : ").append(order.getTotalAmountCfa()).append(" CFA");
        msg.append("\nPays : ").append(country.toUpperCase());
        msg.append("\nOption : ").append("PICKUP".equals(deliveryOption) ? "Retrait en boutique" : "Livraison à domicile");
        msg.append("\n\n\uD83D\uDCE6 N° de suivi : ").append(order.getTrackingNumber());

        String waUrl = "https://wa.me/" + whatsappNumber
                + "?text=" + URLEncoder.encode(msg.toString(), StandardCharsets.UTF_8);

        return "redirect:" + waUrl;
    }
}
