package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
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
}
