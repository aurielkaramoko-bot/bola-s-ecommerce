package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.model.CustomerOrder;
import com.bolas.ecommerce.repository.CustomerOrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API publique — polling du suivi livraison.
 * Appelé toutes les 30 s par track-delivery.html via fetch().
 * Route permise sans authentification dans SecurityConfig : /api/public/**
 */
@RestController
@RequestMapping("/api/public")
public class PublicTrackingApiController {

    private final CustomerOrderRepository orderRepository;

    public PublicTrackingApiController(CustomerOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * GET /api/public/tracking/{trackingNumber}
     * Retourne les infos livreur + position GPS pour le polling JS.
     */
    @GetMapping("/tracking/{trackingNumber}")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> trackingData(
            @PathVariable String trackingNumber) {

        var opt = orderRepository.findByTrackingNumber(trackingNumber.trim());
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CustomerOrder o = opt.get();
        Map<String, Object> data = new LinkedHashMap<>();

        // Statut
        data.put("status",              o.getStatus() != null ? o.getStatus().name() : null);

        // Infos livreur (mis à jour par le vendeur)
        data.put("courierPhone",         o.getCourierPhone());
        data.put("courierVehiclePlate",  o.getCourierVehiclePlate());
        data.put("courierPhotoUrl",      o.getCourierPhotoUrl());

        // GPS livreur (mis à jour par l'app livreur ou le vendeur)
        data.put("courierLatitude",      o.getCourierLatitude());
        data.put("courierLongitude",     o.getCourierLongitude());

        // GPS client (enregistré à la commande)
        data.put("clientLatitude",       o.getClientLatitude());
        data.put("clientLongitude",      o.getClientLongitude());

        return ResponseEntity.ok(data);
    }
}
