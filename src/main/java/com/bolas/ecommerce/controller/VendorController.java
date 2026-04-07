package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.model.CustomerOrder;
import com.bolas.ecommerce.model.OrderStatus;
import com.bolas.ecommerce.repository.CustomerOrderRepository;
import com.bolas.ecommerce.service.OrderFlowService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/vendor")
public class VendorController {

    private final CustomerOrderRepository orderRepository;
    private final OrderFlowService orderFlowService;

    public VendorController(CustomerOrderRepository orderRepository,
                            OrderFlowService orderFlowService) {
        this.orderRepository = orderRepository;
        this.orderFlowService = orderFlowService;
    }

    @GetMapping("/orders")
    public String orders(Model model) {
        // Le vendeur voit uniquement les commandes CONFIRMED (à préparer)
        List<CustomerOrder> toProcess = orderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.CONFIRMED);
        List<CustomerOrder> done = orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.READY);
        model.addAttribute("pageTitle", "Mes commandes — Vendeur Bola's");
        model.addAttribute("toProcess", toProcess);
        model.addAttribute("done", done);
        return "vendor/orders";
    }

    /** Vendeur marque la commande comme prête → notifie l'admin via WhatsApp */
    @PostMapping("/orders/{id}/ready")
    public String markReady(@PathVariable Long id,
                            HttpServletRequest request,
                            RedirectAttributes ra) {
        CustomerOrder order = orderRepository.findById(id).orElseThrow();
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            ra.addFlashAttribute("flashError", "Cette commande ne peut pas être marquée prête.");
            return "redirect:/vendor/orders";
        }
        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() != 80 && request.getServerPort() != 443
                ? ":" + request.getServerPort() : "");
        String waLink = orderFlowService.markReady(order, baseUrl);
        ra.addFlashAttribute("flashOk", "Commande marquée prête !");
        ra.addFlashAttribute("waNotifyLink", waLink);
        return "redirect:/vendor/orders";
    }
}
