package com.bolas.ecommerce.api;

import com.bolas.ecommerce.dto.NotificationDto;
import com.bolas.ecommerce.model.Customer;
import com.bolas.ecommerce.model.Notification;
import com.bolas.ecommerce.model.NotificationDestinataire;
import com.bolas.ecommerce.model.VendorUser;
import com.bolas.ecommerce.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API légère pour les notifications in-app BOLA.
 * Polling toutes les 30s depuis le frontend — pas de WebSocket.
 *
 * Sécurité : chaque endpoint détecte l'utilisateur depuis la session HTTP
 * (client) ou Spring Security (vendor/admin).
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationApiController {

    private final NotificationService notifService;

    public NotificationApiController(NotificationService notifService) {
        this.notifService = notifService;
    }

    /** GET /api/notifications/count → {"count": 5} — utilisé par le badge */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> count(HttpSession session) {
        UserContext ctx = resolveUser(session);
        if (ctx == null) return ResponseEntity.ok(Map.of("count", 0L));
        long c = notifService.countUnread(ctx.id(), ctx.type());
        return ResponseEntity.ok(Map.of("count", c));
    }

    /** GET /api/notifications/recent → 5 dernières notifications (JSON) */
    @GetMapping("/recent")
    public ResponseEntity<List<NotificationDto>> recent(HttpSession session) {
        UserContext ctx = resolveUser(session);
        if (ctx == null) return ResponseEntity.ok(List.of());
        List<NotificationDto> dtos = notifService.getRecent(ctx.id(), ctx.type())
                .stream().map(NotificationDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /** POST /api/notifications/{id}/read → marquer une notif comme lue */
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        notifService.marquerLue(id);
        return ResponseEntity.ok().build();
    }

    /** POST /api/notifications/read-all → tout marquer comme lu */
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(HttpSession session) {
        UserContext ctx = resolveUser(session);
        if (ctx != null) notifService.marquerToutesLues(ctx.id(), ctx.type());
        return ResponseEntity.ok().build();
    }

    // ─── Alias vendeur (même logique, URL différente pour le JS) ─────────────

    @GetMapping("/vendor/count")
    public ResponseEntity<Map<String, Long>> vendorCount(HttpSession session) {
        return count(session);
    }

    @GetMapping("/vendor/recent")
    public ResponseEntity<List<NotificationDto>> vendorRecent(HttpSession session) {
        return recent(session);
    }

    // ─── Résolution de l'utilisateur courant ─────────────────────────────────

    private UserContext resolveUser(HttpSession session) {
        // Client connecté via session
        Object customer = session.getAttribute("BOLAS_CUSTOMER");
        if (customer instanceof Customer c) {
            return new UserContext(c.getId(), NotificationDestinataire.CLIENT);
        }
        // Vendeur connecté via session (BOLAS_VENDOR)
        Object vendor = session.getAttribute("BOLAS_VENDOR");
        if (vendor instanceof VendorUser v) {
            return new UserContext(v.getId(), NotificationDestinataire.VENDEUR);
        }
        // Vendeur via Spring Security principal
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof VendorUser v) {
            return new UserContext(v.getId(), NotificationDestinataire.VENDEUR);
        }
        // Admin
        if (auth != null && auth.isAuthenticated()
                && auth.getAuthorities().stream()
                       .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return new UserContext(1L, NotificationDestinataire.ADMIN);
        }
        return null;
    }

    record UserContext(Long id, NotificationDestinataire type) {}
}
