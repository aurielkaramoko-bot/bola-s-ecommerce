package com.bolas.ecommerce.api;

import com.bolas.ecommerce.model.CustomerOrder;
import com.bolas.ecommerce.model.OrderStatus;
import com.bolas.ecommerce.repository.CustomerOrderRepository;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Reçoit la position GPS du livreur depuis son téléphone.
 * Protégé par token UUID unique par commande — pas de session, pas de login.
 */
@RestController
@Validated
public class CourierGpsApiController {

    private final CustomerOrderRepository orderRepository;

    public CourierGpsApiController(CustomerOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @PostMapping("/api/livreur/{token}/position")
    public ResponseEntity<?> updatePosition(
            @PathVariable String token,
            @RequestParam @NotNull
            @DecimalMin(value = "-90.0")  @DecimalMax(value = "90.0")  Double lat,
            @RequestParam @NotNull
            @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") Double lng) {

        // Validation basique du token (UUID format)
        if (token == null || token.length() > 64 || !token.matches("[a-f0-9\\-]{36,64}")) {
            return ResponseEntity.badRequest().body(Map.of("error", "token_invalide"));
        }

        return orderRepository.findByCourierToken(token)
                .filter(o -> o.getStatus() == OrderStatus.IN_DELIVERY)
                .map(order -> {
                    order.setCourierLatitude(lat);
                    order.setCourierLongitude(lng);
                    orderRepository.save(order);
                    return ResponseEntity.ok(Map.of("ok", true));
                })
                .orElse(ResponseEntity.status(403).body(Map.of("error", "token_invalide_ou_commande_non_active")));
    }
}
