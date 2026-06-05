package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.model.NotificationDestinataire;
import com.bolas.ecommerce.model.VendorUser;
import com.bolas.ecommerce.repository.ProductRepository;
import com.bolas.ecommerce.service.CartService;
import com.bolas.ecommerce.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "com.bolas.ecommerce.controller")
public class GlobalModelAdvice {

    private final CartService cartService;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    public GlobalModelAdvice(CartService cartService,
                             ProductRepository productRepository,
                             NotificationService notificationService) {
        this.cartService = cartService;
        this.productRepository = productRepository;
        this.notificationService = notificationService;
    }

    @ModelAttribute("whatsappNumber")
    public String whatsappNumber(@Value("${whatsapp.number}") String number) {
        return number != null ? number : "";
    }

    @ModelAttribute("shopPhone")
    public String shopPhone(@Value("${bolas.shop.phone:}") String phone) {
        return phone != null ? phone : "";
    }

    @ModelAttribute("shopEmail")
    public String shopEmail(@Value("${bolas.shop.email:}") String email) {
        return email != null ? email : "";
    }

    @ModelAttribute("shopName")
    public String shopName(@Value("${bolas.shop.name:BOLA}") String name) {
        return name != null ? name : "BOLA";
    }

    @ModelAttribute("cartItemCount")
    public int cartItemCount(HttpSession session) {
        return cartService.totalItems(session);
    }

    @ModelAttribute("popularProducts")
    public java.util.List<com.bolas.ecommerce.model.Product> popularProducts() {
        try {
            return productRepository.findPopularForHomepage();
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    /** Badge notifications non lues — injecté pour tous les vendeurs connectés (cloche navbar) */
    @ModelAttribute("unreadNotifCount")
    public long unreadNotifCount(HttpSession session) {
        try {
            Object obj = session.getAttribute("BOLAS_VENDOR");
            if (obj instanceof VendorUser v) {
                return notificationService.countUnread(v.getId(), NotificationDestinataire.VENDEUR);
            }
        } catch (Exception ignored) {}
        return 0L;
    }

    /** 5 dernières notifications pour le dropdown cloche vendeur */
    @ModelAttribute("recentNotifs")
    public java.util.List<com.bolas.ecommerce.model.Notification> recentNotifs(HttpSession session) {
        try {
            Object obj = session.getAttribute("BOLAS_VENDOR");
            if (obj instanceof VendorUser v) {
                return notificationService.getRecent(v.getId(), NotificationDestinataire.VENDEUR);
            }
        } catch (Exception ignored) {}
        return java.util.Collections.emptyList();
    }
}

