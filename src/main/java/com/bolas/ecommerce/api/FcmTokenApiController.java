package com.bolas.ecommerce.api;

import com.bolas.ecommerce.model.*;
import com.bolas.ecommerce.service.FcmService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * API pour enregistrer/supprimer un token FCM depuis le frontend.
 * Appelé au chargement de la page après permission notification.
 */
@RestController
@RequestMapping("/api/fcm")
public class FcmTokenApiController {

    private final FcmService fcmService;

    public FcmTokenApiController(FcmService fcmService) {
        this.fcmService = fcmService;
    }

    /** POST /api/fcm/token — enregistre le token FCM de l'utilisateur connecté */
    @PostMapping("/token")
    public ResponseEntity<Map<String, Boolean>> registerToken(
            @RequestBody Map<String, String> body,
            HttpSession session) {

        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false));
        }

        // Détecter le type d'utilisateur connecté
        Object customer = session.getAttribute("BOLAS_CUSTOMER");
        Object vendor   = session.getAttribute("BOLAS_VENDOR");
        Object livreur  = session.getAttribute("BOLAS_LIVREUR");

        if (customer instanceof Customer c) {
            fcmService.saveToken(c.getId(), NotificationDestinataire.CLIENT, token);
        } else if (vendor instanceof VendorUser v) {
            fcmService.saveToken(v.getId(), NotificationDestinataire.VENDEUR, token);
        } else if (livreur instanceof com.bolas.ecommerce.model.Livreur l) {
            // Livreur — on utilise LIVREUR si disponible sinon CLIENT
            fcmService.saveToken(l.getId(), NotificationDestinataire.CLIENT, token);
        } else {
            return ResponseEntity.ok(Map.of("ok", false)); // non connecté
        }

        return ResponseEntity.ok(Map.of("ok", true));
    }
}
