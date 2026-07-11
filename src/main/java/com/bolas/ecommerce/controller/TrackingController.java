package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.repository.CustomerOrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class TrackingController {

    private final CustomerOrderRepository customerOrderRepository;

    @Value("${google.maps.api.key:}")
    private String googleMapsApiKey;

    @Value("${bolas.shop.latitude:5.3600}")
    private double shopLatitude;

    @Value("${bolas.shop.longitude:-3.9903}")
    private double shopLongitude;

    public TrackingController(CustomerOrderRepository customerOrderRepository) {
        this.customerOrderRepository = customerOrderRepository;
    }

    @GetMapping("/tracking")
    public String track(@RequestParam(required = false) String trackingNumber, Model model) {
        model.addAttribute("pageTitle", "Suivi livraison — Bola's");
        // La clé Maps est chargée via /api/maps-config côté JS, pas exposée dans le HTML
        model.addAttribute("shopLatitude", shopLatitude);
        model.addAttribute("shopLongitude", shopLongitude);
        model.addAttribute("trackingNumber", trackingNumber);

        if (trackingNumber != null && !trackingNumber.isBlank()) {
            var opt = customerOrderRepository.findByTrackingNumber(trackingNumber.trim());
            if (opt.isPresent()) {
                model.addAttribute("tracking", opt.get());
            } else {
                model.addAttribute("trackingError", "Aucune commande ne correspond à ce numéro.");
            }
        }
        return "track-delivery";
    }

    /** Endpoint JS pour charger la clé Maps sans l'exposer dans le HTML source */
    @GetMapping("/api/maps-config")
    @ResponseBody
    public ResponseEntity<Map<String, String>> mapsConfig() {
        if (googleMapsApiKey == null || googleMapsApiKey.isBlank()) {
            return ResponseEntity.ok(Map.of("key", ""));
        }
        return ResponseEntity.ok(Map.of("key", googleMapsApiKey));
    }
}
