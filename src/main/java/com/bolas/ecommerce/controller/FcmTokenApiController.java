package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.dto.FcmTokenDto;
import com.bolas.ecommerce.model.Customer;
import com.bolas.ecommerce.model.NotificationDestinataire;
import com.bolas.ecommerce.model.VendorUser;
import com.bolas.ecommerce.service.FcmService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoint REST pour enregistrer le token FCM du navigateur.
 * Appelé automatiquement après la demande de permission notifications.
 */
@RestController
@RequestMapping("/api/fcm")
public class FcmTokenApiController {

    private static final Logger log = LoggerFactory.getLogger(FcmTokenApiController.class);

    private final FcmService fcmService;

    public FcmTokenApiController(FcmService fcmService) {
        this.fcmService = fcmService;
    }

    /**
     * POST /api/fcm/token
     * Body : { "token": "fcm-token-string" }
     * Détecte automatiquement si c'est un vendeur ou un client connecté.
     */
    @PostMapping("/token")
    public ResponseEntity<Map<String, String>> registerToken(
            @Valid @RequestBody FcmTokenDto body,
            HttpSession session) {

        String token = body.getToken();

        // Vendeur connecté ?
        Object vendorObj = session.getAttribute("BOLAS_VENDOR");
        if (vendorObj instanceof VendorUser vendor) {
            try {
                fcmService.saveToken(vendor.getId(), NotificationDestinataire.VENDEUR, token);
                log.debug("FCM token enregistré pour vendeur {}", vendor.getId());
                return ResponseEntity.ok(Map.of("status", "ok", "type", "VENDOR"));
            } catch (Exception e) {
                log.warn("Erreur enregistrement FCM vendeur: {}", e.getMessage());
            }
        }

        // Client connecté ?
        Object customerObj = session.getAttribute("BOLAS_CUSTOMER");
        if (customerObj instanceof Customer customer) {
            try {
                fcmService.saveToken(customer.getId(), NotificationDestinataire.CLIENT, token);
                log.debug("FCM token enregistré pour client {}", customer.getId());
                return ResponseEntity.ok(Map.of("status", "ok", "type", "CLIENT"));
            } catch (Exception e) {
                log.warn("Erreur enregistrement FCM client: {}", e.getMessage());
            }
        }

        // Utilisateur non connecté — on accepte quand même (il sera lié lors du login)
        return ResponseEntity.ok(Map.of("status", "ok", "type", "ANONYMOUS"));
    }
}
