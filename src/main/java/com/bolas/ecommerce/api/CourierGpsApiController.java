package com.bolas.ecommerce.api;

import com.bolas.ecommerce.model.OrderStatus;
import com.bolas.ecommerce.repository.CustomerOrderRepository;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@Validated
public class CourierGpsApiController {

    private final CustomerOrderRepository orderRepository;

    public CourierGpsApiController(CustomerOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @PostMapping("/api/livreur/{token}/position")
    public ResponseEntity<Map<String, Object>> updatePosition(
            @PathVariable String token,
            @RequestParam @NotNull
            @DecimalMin(value = "-90.0")  @DecimalMax(value = "90.0")  Double lat,
            @RequestParam @NotNull
            @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") Double lng) {

        if (token == null || token.length() > 64 || !token.matches("[a-f0-9\\-]{36,64}")) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "token_invalide");
            return ResponseEntity.badRequest().body(err);
        }

        var opt = orderRepository.findByCourierToken(token);
        if (opt.isEmpty() || opt.get().getStatus() != OrderStatus.IN_DELIVERY) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "token_invalide_ou_commande_non_active");
            return ResponseEntity.status(403).body(err);
        }

        var order = opt.get();
        order.setCourierLatitude(lat);
        order.setCourierLongitude(lng);
        orderRepository.save(order);

        Map<String, Object> ok = new LinkedHashMap<>();
        ok.put("ok", true);
        return ResponseEntity.ok(ok);
    }
}
