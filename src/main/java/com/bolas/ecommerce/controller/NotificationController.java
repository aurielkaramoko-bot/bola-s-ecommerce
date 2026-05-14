package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.model.Customer;
import com.bolas.ecommerce.model.Notification;
import com.bolas.ecommerce.model.NotificationDestinataire;
import com.bolas.ecommerce.model.VendorUser;
import com.bolas.ecommerce.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** Contrôleur pour la page complète /notifications */
@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notifService;

    public NotificationController(NotificationService notifService) {
        this.notifService = notifService;
    }

    @GetMapping
    public String page(HttpSession session, Model model) {
        UserCtx ctx = resolve(session);
        if (ctx == null) return "redirect:/customer/login";

        List<Notification> notifs = notifService.getAll(ctx.id(), ctx.type());
        long unread = notifService.countUnread(ctx.id(), ctx.type());

        model.addAttribute("notifications", notifs);
        model.addAttribute("unreadCount", unread);
        model.addAttribute("pageTitle", "Mes notifications — BOLA");
        return "notifications";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session,
                         RedirectAttributes ra) {
        notifService.supprimer(id);
        ra.addFlashAttribute("flashOk", "Notification supprimée.");
        return "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String readAll(HttpSession session) {
        UserCtx ctx = resolve(session);
        if (ctx != null) notifService.marquerToutesLues(ctx.id(), ctx.type());
        return "redirect:/notifications";
    }

    // ─── Résolution utilisateur ───────────────────────────────────────────────

    private UserCtx resolve(HttpSession session) {
        Object customer = session.getAttribute("BOLAS_CUSTOMER");
        if (customer instanceof Customer c) return new UserCtx(c.getId(), NotificationDestinataire.CLIENT);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof VendorUser v)
            return new UserCtx(v.getId(), NotificationDestinataire.VENDEUR);
        if (auth != null && auth.isAuthenticated()
                && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")))
            return new UserCtx(1L, NotificationDestinataire.ADMIN);
        return null;
    }

    record UserCtx(Long id, NotificationDestinataire type) {}
}
