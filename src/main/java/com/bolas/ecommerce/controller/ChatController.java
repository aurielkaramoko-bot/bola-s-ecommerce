package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.model.ChatMessage;
import com.bolas.ecommerce.model.Customer;
import com.bolas.ecommerce.model.VendorUser;
import com.bolas.ecommerce.repository.ChatMessageRepository;
import com.bolas.ecommerce.repository.VendorUserRepository;
import com.bolas.ecommerce.service.InputSanitizerService;
import com.bolas.ecommerce.service.MetaWhatsAppService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.util.List;

/**
 * Chat côté acheteur : permet à un client connecté d'envoyer des messages à un vendeur.
 */
@Controller
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final VendorUserRepository vendorUserRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final InputSanitizerService sanitizer;
    private final MetaWhatsAppService metaWhatsApp;

    public ChatController(VendorUserRepository vendorUserRepository,
                          ChatMessageRepository chatMessageRepository,
                          InputSanitizerService sanitizer,
                          MetaWhatsAppService metaWhatsApp) {
        this.vendorUserRepository = vendorUserRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.sanitizer = sanitizer;
        this.metaWhatsApp = metaWhatsApp;
    }

    /** Récupère le client en session */
    private Customer currentCustomer(HttpSession session) {
        Object obj = session.getAttribute("BOLAS_CUSTOMER");
        return obj instanceof Customer c ? c : null;
    }

    /** Page de conversation client → vendeur */
    @GetMapping("/chat/{vendorId}")
    public String chatPage(@PathVariable Long vendorId,
                           HttpSession session,
                           Model model,
                           RedirectAttributes ra) {
        Customer customer = currentCustomer(session);
        if (customer == null) {
            ra.addFlashAttribute("flashError", "Connectez-vous pour envoyer un message.");
            return "redirect:/customer/login";
        }

        VendorUser vendor = vendorUserRepository.findById(vendorId).orElse(null);
        if (vendor == null) {
            ra.addFlashAttribute("flashError", "Vendeur introuvable.");
            return "redirect:/products";
        }

        // Chat bloqué si le vendeur n'a pas un plan PRO/PREMIUM
        if (!vendor.canChat()) {
            ra.addFlashAttribute("flashError",
                    "Ce vendeur n'a pas encore activé le chat. Contactez-le via WhatsApp.");
            return "redirect:/boutiques/" + vendorId;
        }

        String custId = customer.getEmail();
        List<ChatMessage> messages = chatMessageRepository
                .findByVendorAndCustomerIdentifierOrderBySentAtAsc(vendor, custId);

        // Marquer comme lus par le client
        messages.stream()
                .filter(m -> "VENDOR".equals(m.getSenderType()) && !m.isReadByCustomer())
                .forEach(m -> { m.setReadByCustomer(true); chatMessageRepository.save(m); });

        model.addAttribute("pageTitle", "Chat avec " + vendor.getDisplayName() + " — BOLA");
        model.addAttribute("vendor", vendor);
        model.addAttribute("customer", customer);
        model.addAttribute("messages", messages);
        return "chat";
    }

    /** Envoi d'un message client → vendeur */
    @PostMapping("/chat/{vendorId}/send")
    public String sendMessage(@PathVariable Long vendorId,
                              @RequestParam String message,
                              HttpSession session,
                              RedirectAttributes ra) {
        Customer customer = currentCustomer(session);
        if (customer == null) return "redirect:/customer/login";

        VendorUser vendor = vendorUserRepository.findById(vendorId).orElse(null);
        if (vendor == null) return "redirect:/products";

        if (message == null || message.isBlank()) {
            return "redirect:/chat/" + vendorId;
        }

        String sanitized = sanitizer.sanitizeText(message);
        if (sanitized == null || sanitized.isBlank()) {
            return "redirect:/chat/" + vendorId;
        }

        ChatMessage msg = new ChatMessage();
        msg.setVendor(vendor);
        msg.setCustomerIdentifier(customer.getEmail());
        msg.setCustomerName(customer.getFirstName() + " " + customer.getLastName());
        msg.setSenderType("CUSTOMER");
        msg.setMessage(sanitized);
        msg.setSentAt(Instant.now());
        msg.setReadByCustomer(true);
        msg.setReadByVendor(false);
        chatMessageRepository.save(msg);

        // Notifier le vendeur via WhatsApp si téléphone disponible
        try {
            if (vendor.getPhone() != null && !vendor.getPhone().isBlank()) {
                String customerDisplay = customer.getFirstName() + " " + customer.getLastName();
                String notif = "💬 Nouveau message de " + customerDisplay + " sur BOLA !\n\n"
                        + "\"" + sanitized + "\"\n\n"
                        + "→ Répondez depuis votre espace vendeur : Messages";
                metaWhatsApp.sendText(vendor.getPhone(), notif);
                log.info("✅ Notif WhatsApp chat envoyée au vendeur {}", vendor.getDisplayName());
            }
        } catch (Exception e) {
            log.warn("⚠️ Notif WhatsApp chat échouée pour vendeur {} : {}", vendor.getDisplayName(), e.getMessage());
        }

        return "redirect:/chat/" + vendorId;
    }
}
