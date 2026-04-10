package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.model.CustomerOrder;
import com.bolas.ecommerce.model.OrderStatus;
import com.bolas.ecommerce.repository.CustomerOrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class CourierController {

    private final CustomerOrderRepository orderRepository;

    public CourierController(CustomerOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping("/livreur/{token}")
    public String courierPage(@PathVariable String token, Model model) {
        // Validation format token
        if (token == null || token.length() > 64 || !token.matches("[a-f0-9\\-]{36,64}")) {
            return "redirect:/";
        }

        return orderRepository.findByCourierToken(token)
                .map(order -> {
                    boolean active = order.getStatus() == OrderStatus.IN_DELIVERY;
                    model.addAttribute("pageTitle", "Livraison — Bola's");
                    model.addAttribute("token", token);
                    model.addAttribute("tracking", order.getTrackingNumber());
                    model.addAttribute("customerName", order.getCustomerName());
                    model.addAttribute("customerPhone", order.getCustomerPhone());
                    model.addAttribute("customerAddress", order.getCustomerAddress());
                    model.addAttribute("clientLatitude", order.getClientLatitude());
                    model.addAttribute("clientLongitude", order.getClientLongitude());
                    model.addAttribute("active", active);
                    return "livreur";
                })
                .orElse("redirect:/");
    }
}
