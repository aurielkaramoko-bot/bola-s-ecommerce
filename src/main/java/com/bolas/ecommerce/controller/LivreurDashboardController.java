package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.model.*;
import com.bolas.ecommerce.repository.*;
import com.bolas.ecommerce.service.*;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Controller
@RequestMapping("/livreur")
public class LivreurDashboardController {

    private static final Logger log = LoggerFactory.getLogger(LivreurDashboardController.class);
    private static final String SESSION_KEY = "BOLAS_LIVREUR";

    private final LivreurRepository livreurRepo;
    private final CustomerOrderRepository orderRepo;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final ImageUploadService imageUploadService;
    private final CustomerRepository customerRepo;

    @Value("${app.base-url:https://bola-s-ecommerce.onrender.com}")
    private String appBaseUrl;

    public LivreurDashboardController(LivreurRepository livreurRepo,
                                       CustomerOrderRepository orderRepo,
                                       PasswordEncoder passwordEncoder,
                                       NotificationService notificationService,
                                       ImageUploadService imageUploadService,
                                       CustomerRepository customerRepo) {
        this.livreurRepo = livreurRepo;
        this.orderRepo = orderRepo;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.imageUploadService = imageUploadService;
        this.customerRepo = customerRepo;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Livreur current(HttpSession session) {
        Object obj = session.getAttribute(SESSION_KEY);
        if (!(obj instanceof Livreur l)) return null;
        return livreurRepo.findById(l.getId()).filter(Livreur::isActive).orElse(null);
    }

    private String require(HttpSession session) {
        if (current(session) == null) return "redirect:/livreur/login";
        return null;
    }

    // ─── Inscription ─────────────────────────────────────────────────────────

    @GetMapping("/register")
    public String registerPage(HttpSession session, Model model) {
        if (current(session) != null) return "redirect:/livreur/dashboard";
        model.addAttribute("pageTitle", "Devenir livreur — BOLA");
        return "livreur/register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name,
                           @RequestParam String phone,
                           @RequestParam String pin,
                           @RequestParam(required = false) String vehiclePlate,
                           @RequestParam(required = false) String vehicleType,
                           @RequestParam(required = false) String deliveryZone,
                           @RequestParam(value = "vehiclePhoto", required = false) MultipartFile vehiclePhoto,
                           RedirectAttributes ra) {
        if (name == null || name.isBlank() || phone == null || phone.isBlank()) {
            ra.addFlashAttribute("flashError", "Nom et téléphone obligatoires.");
            return "redirect:/livreur/register";
        }
        if (pin == null || pin.length() < 4) {
            ra.addFlashAttribute("flashError", "Le PIN doit faire au moins 4 chiffres.");
            return "redirect:/livreur/register";
        }
        if (livreurRepo.existsByPhone(phone.trim())) {
            ra.addFlashAttribute("flashError", "Ce numéro est déjà enregistré.");
            return "redirect:/livreur/register";
        }
        Livreur l = new Livreur();
        l.setName(name.trim());
        l.setPhone(phone.trim());
        l.setPinHash(passwordEncoder.encode(pin));
        l.setVehiclePlate(vehiclePlate != null ? vehiclePlate.trim() : null);
        l.setVehicleType(vehicleType);
        l.setDeliveryZone(deliveryZone != null ? deliveryZone.trim() : null);
        if (vehiclePhoto != null && !vehiclePhoto.isEmpty()) {
            try { l.setVehiclePhotoUrl(imageUploadService.store(vehiclePhoto)); }
            catch (Exception e) { log.warn("Photo véhicule upload failed: {}", e.getMessage()); }
        }
        livreurRepo.save(l);
        ra.addFlashAttribute("flashOk", "Compte créé ! Connectez-vous.");
        return "redirect:/livreur/login";
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        if (current(session) != null) return "redirect:/livreur/dashboard";
        model.addAttribute("pageTitle", "Connexion livreur — BOLA");
        return "livreur/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String phone,
                        @RequestParam String pin,
                        HttpSession session,
                        RedirectAttributes ra) {
        Livreur l = livreurRepo.findByPhone(phone.trim()).orElse(null);
        if (l == null || !l.isActive() || !passwordEncoder.matches(pin, l.getPinHash())) {
            ra.addFlashAttribute("flashError", "Téléphone ou PIN incorrect.");
            return "redirect:/livreur/login";
        }
        session.setAttribute(SESSION_KEY, l);
        return "redirect:/livreur/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
        return "redirect:/livreur/login";
    }

    // ─── Dashboard ────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public String dashboard(HttpSession session, Model model) {
        String redir = require(session);
        if (redir != null) return redir;
        Livreur livreur = current(session);

        // Commandes assignées en cours (IN_DELIVERY)
        List<CustomerOrder> active = orderRepo.findByAssignedCourierNameAndStatus(
                livreur.getName(), OrderStatus.IN_DELIVERY);
        // Commandes assignées en attente (READY)
        List<CustomerOrder> pending = orderRepo.findByAssignedCourierNameAndStatus(
                livreur.getName(), OrderStatus.READY);
        // Historique (DELIVERED)
        List<CustomerOrder> history = orderRepo.findTop10ByAssignedCourierNameOrderByCreatedAtDesc(
                livreur.getName()).stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED).toList();

        // Revenus du mois (commandes livrées ce mois)
        Instant startOfMonth = Instant.now().truncatedTo(ChronoUnit.DAYS)
                .minus(30, ChronoUnit.DAYS);
        long revenueCount = orderRepo.countByAssignedCourierNameAndStatus(
                livreur.getName(), OrderStatus.DELIVERED);

        model.addAttribute("pageTitle", "Mon espace livreur — BOLA");
        model.addAttribute("livreur", livreur);
        model.addAttribute("activeDeliveries", active);
        model.addAttribute("pendingDeliveries", pending);
        model.addAttribute("history", history);
        model.addAttribute("totalDelivered", revenueCount);
        return "livreur/dashboard";
    }

    // ─── Confirmer prise en charge ────────────────────────────────────────────

    @PostMapping("/orders/{id}/accept")
    @Transactional
    public String acceptOrder(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        String redir = require(session);
        if (redir != null) return redir;
        Livreur livreur = current(session);

        CustomerOrder order = orderRepo.findById(id).orElse(null);
        if (order == null || !livreur.getName().equals(order.getAssignedCourierName())) {
            ra.addFlashAttribute("flashError", "Commande introuvable.");
            return "redirect:/livreur/dashboard";
        }
        if (order.getStatus() != OrderStatus.READY) {
            ra.addFlashAttribute("flashError", "Cette commande n'est pas en attente de prise en charge.");
            return "redirect:/livreur/dashboard";
        }
        order.setStatus(OrderStatus.IN_DELIVERY);
        orderRepo.save(order);

        // Notifier le client
        notifyClient(order, "Votre commande est en cours de livraison !",
                "🚚 Votre livreur " + livreur.getName() + " a pris en charge votre commande #" + order.getTrackingNumber());
        ra.addFlashAttribute("flashOk", "Prise en charge confirmée !");
        return "redirect:/livreur/dashboard";
    }

    // ─── Marquer livré ────────────────────────────────────────────────────────

    @PostMapping("/orders/{id}/delivered")
    @Transactional
    public String markDelivered(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        String redir = require(session);
        if (redir != null) return redir;
        Livreur livreur = current(session);

        CustomerOrder order = orderRepo.findById(id).orElse(null);
        if (order == null || !livreur.getName().equals(order.getAssignedCourierName())) {
            ra.addFlashAttribute("flashError", "Commande introuvable.");
            return "redirect:/livreur/dashboard";
        }
        if (order.getStatus() != OrderStatus.IN_DELIVERY) {
            ra.addFlashAttribute("flashError", "Cette commande n'est pas en livraison.");
            return "redirect:/livreur/dashboard";
        }
        order.setStatus(OrderStatus.DELIVERED);
        orderRepo.save(order);

        // Notifier client
        notifyClient(order, "Commande livrée !",
                "✅ Votre commande #" + order.getTrackingNumber() + " a été livrée. Merci !");

        // Notifier vendeur
        if (order.getVendor() != null) {
            notificationService.envoyer(
                order.getVendor().getId(), NotificationDestinataire.VENDEUR,
                NotificationType.LIVRAISON,
                "Commande livrée",
                "#" + order.getTrackingNumber() + " livrée par " + livreur.getName(),
                "/vendor/orders"
            );
        }

        ra.addFlashAttribute("flashOk", "Commande marquée comme livrée !");
        return "redirect:/livreur/dashboard";
    }

    // ─── Profil ───────────────────────────────────────────────────────────────

    @GetMapping("/profile")
    public String profilePage(HttpSession session, Model model) {
        String redir = require(session);
        if (redir != null) return redir;
        model.addAttribute("pageTitle", "Mon profil livreur — BOLA");
        model.addAttribute("livreur", current(session));
        return "livreur/profile";
    }

    @PostMapping("/profile")
    @Transactional
    public String updateProfile(@RequestParam String name,
                                @RequestParam(required = false) String vehiclePlate,
                                @RequestParam(required = false) String vehicleType,
                                @RequestParam(required = false) String deliveryZone,
                                @RequestParam(value = "vehiclePhoto", required = false) MultipartFile vehiclePhoto,
                                HttpSession session,
                                RedirectAttributes ra) {
        String redir = require(session);
        if (redir != null) return redir;
        Livreur l = livreurRepo.findById(current(session).getId()).orElseThrow();
        l.setName(name.trim());
        l.setVehiclePlate(vehiclePlate != null ? vehiclePlate.trim() : null);
        l.setVehicleType(vehicleType);
        l.setDeliveryZone(deliveryZone != null ? deliveryZone.trim() : null);
        if (vehiclePhoto != null && !vehiclePhoto.isEmpty()) {
            try { l.setVehiclePhotoUrl(imageUploadService.store(vehiclePhoto)); }
            catch (Exception e) { log.warn("Photo upload: {}", e.getMessage()); }
        }
        livreurRepo.save(l);
        session.setAttribute(SESSION_KEY, l);
        ra.addFlashAttribute("flashOk", "Profil mis à jour !");
        return "redirect:/livreur/profile";
    }

    // ─── Helper notifications ─────────────────────────────────────────────────

    private void notifyClient(CustomerOrder order, String titre, String message) {
        try {
            customerRepo.findByPhone(order.getCustomerPhone()).ifPresent(c ->
                notificationService.envoyer(c.getId(), NotificationDestinataire.CLIENT,
                    NotificationType.LIVRAISON, titre, message,
                    "/tracking?trackingNumber=" + order.getTrackingNumber())
            );
        } catch (Exception e) {
            log.warn("Notif client livreur: {}", e.getMessage());
        }
    }
}
