package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "com.bolas.ecommerce.controller")
public class GlobalModelAdvice {

    private final CartService cartService;

    public GlobalModelAdvice(CartService cartService) {
        this.cartService = cartService;
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
}
