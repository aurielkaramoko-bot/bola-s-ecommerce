package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.repository.CustomerOrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TrackingController {

    private final CustomerOrderRepository customerOrderRepository;

    @Value("${google.maps.api.key}")
    private String googleMapsApiKey;

    @Value("${bolas.shop.latitude}")
    private double shopLatitude;

    @Value("${bolas.shop.longitude}")
    private double shopLongitude;

    public TrackingController(CustomerOrderRepository customerOrderRepository) {
        this.customerOrderRepository = customerOrderRepository;
    }

    @GetMapping("/tracking")
    public String track(@RequestParam(required = false) String trackingNumber, Model model) {
        model.addAttribute("pageTitle", "Suivi livraison — Bola's");
        model.addAttribute("googleMapsApiKey", googleMapsApiKey);
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
}
