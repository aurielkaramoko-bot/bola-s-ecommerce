package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.model.CustomerOrder;
import com.bolas.ecommerce.model.DeliveryOption;
import com.bolas.ecommerce.model.OrderLine;
import com.bolas.ecommerce.repository.CustomerOrderRepository;
import com.bolas.ecommerce.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
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

    private final CartService cartService;
    private final CustomerOrderRepository orderRepository;
    private final String whatsappNumber;

    public CartController(CartService cartService,
                          CustomerOrderRepository orderRepository,
                          @Value("${whatsapp.number}") String whatsappNumber) {
        this.cartService = cartService;
        this.orderRepository = orderRepository;
        this.whatsappNumber = whatsappNumber;
    }

    @GetMapping("/cart")
    public String cart(Model model, HttpSession session) {
        model.addAttribute("pageTitle", "Panier — Bola's");
        model.addAttribute("cartLines", cartService.lines(session));
        model.addAttribute("cartTotalCfa", cartService.totalAmountCfa(session));
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
    public String checkout(@RequestParam String customerName,
                           @RequestParam String customerPhone,
                           @RequestParam(required = false, defaultValue = "") String customerAddress,
                           @RequestParam(defaultValue = "HOME") String deliveryOption,
                           @RequestParam(required = false) Double clientLatitude,
                           @RequestParam(required = false) Double clientLongitude,
                           HttpSession session,
                           RedirectAttributes ra) {

        var lines = cartService.lines(session);
        if (lines.isEmpty()) {
            ra.addFlashAttribute("flashError", "Votre panier est vide.");
            return "redirect:/cart";
        }

        // Créer la commande
        CustomerOrder order = new CustomerOrder();
        order.setTrackingNumber("BOL-" + UUID.randomUUID().toString().replace("-","").substring(0,8).toUpperCase());
        order.setCustomerName(customerName.trim());
        order.setCustomerPhone(customerPhone.trim());
        order.setCustomerAddress(customerAddress.trim());
        order.setDeliveryOption("PICKUP".equals(deliveryOption) ? DeliveryOption.PICKUP : DeliveryOption.HOME);
        order.setTotalAmountCfa(cartService.totalAmountCfa(session));
        order.setDeliveryFeeCfa(0L);
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

        // Construire le message WhatsApp
        StringBuilder msg = new StringBuilder();
        msg.append("Bonjour Bola's \uD83D\uDC4B\nJe souhaite commander :\n\n");
        for (var line : lines) {
            msg.append("• ").append(line.product().getName())
               .append(" x").append(line.quantity())
               .append(" = ").append(line.lineTotalCfa()).append(" CFA\n");
        }
        msg.append("\nTotal : ").append(order.getTotalAmountCfa()).append(" CFA");
        msg.append("\nOption : ").append("PICKUP".equals(deliveryOption) ? "Retrait en boutique" : "Livraison à domicile");
        msg.append("\n\n\uD83D\uDCE6 N° de suivi : ").append(order.getTrackingNumber());

        String waUrl = "https://wa.me/" + whatsappNumber
                + "?text=" + URLEncoder.encode(msg.toString(), StandardCharsets.UTF_8);

        return "redirect:" + waUrl;
    }
}
