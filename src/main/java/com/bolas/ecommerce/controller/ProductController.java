package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.model.*;
import com.bolas.ecommerce.repository.CustomerOrderRepository;
import com.bolas.ecommerce.repository.CategoryRepository;
import com.bolas.ecommerce.repository.ProductRepository;
import com.bolas.ecommerce.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Controller
public class ProductController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;
    private final CustomerOrderRepository orderRepository;

    private final String whatsappNumber;
    private final String shopPhone;

    public ProductController(ProductRepository productRepository,
                             CategoryRepository categoryRepository,
                             ReviewRepository reviewRepository,
                             CustomerOrderRepository orderRepository,
                             @Value("${whatsapp.number}") String whatsappNumber,
                             @Value("${bolas.shop.phone}") String shopPhone) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.whatsappNumber = whatsappNumber;
        this.shopPhone = shopPhone;
    }

    @GetMapping("/products")
    public String list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long priceMin,
            @RequestParam(required = false) Long priceMax,
            @RequestParam(required = false) String q,
            Model model) {

        long min = (priceMin != null && priceMin >= 0) ? priceMin : 0L;
        long max = (priceMax != null && priceMax >= 0 && priceMax <= 100_000_000L)
                   ? priceMax : Long.MAX_VALUE;
        if (min > max) { min = 0L; max = Long.MAX_VALUE; }

        String keyword = (q != null && !q.isBlank()) ? q.trim() : null;

        List<com.bolas.ecommerce.model.Product> products;
        if (keyword != null) {
            products = categoryId != null
                    ? productRepository.searchByKeywordAndCategory(keyword, categoryId)
                    : productRepository.searchByKeyword(keyword);
        } else {
            products = categoryId != null
                    ? productRepository.findByAvailableTrueAndCategory_IdAndPriceCfaBetween(categoryId, min, max)
                    : productRepository.findByAvailableTrueAndPriceCfaBetween(min, max);
        }

        model.addAttribute("pageTitle", keyword != null
                ? "Résultats pour \"" + keyword + "\" — BOLA"
                : "Produits — BOLA");
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("priceMin", priceMin);
        model.addAttribute("priceMax", priceMax);
        model.addAttribute("q", keyword);
        return "products";
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Product product = productRepository.findById(id).filter(Product::isAvailable).orElseThrow();
        model.addAttribute("pageTitle", product.getName() + " — BOLA");
        model.addAttribute("product", product);
        model.addAttribute("whatsappNumber", whatsappNumber);
        model.addAttribute("shopPhone", shopPhone);

        // Avis produit
        var reviews = reviewRepository.findByProductAndApprovedTrueOrderByCreatedAtDesc(product);
        double avgRating = reviewRepository.averageRatingByProduct(product);
        long reviewCount = reviewRepository.countByProductAndApprovedTrue(product);
        model.addAttribute("reviews", reviews);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("reviewCount", reviewCount);

        // Produits populaires
        model.addAttribute("popularProducts",
                productRepository.findTop6ByAvailableTrueOrderByFeaturedDescIdDesc());

        return "product-detail";
    }

    /**
     * Commande via WhatsApp depuis la fiche produit :
     * crée la commande en base PUIS redirige vers WhatsApp.
     */
    @PostMapping("/products/{id}/whatsapp-order")
    public String whatsappOrder(@PathVariable Long id,
                                @RequestParam(defaultValue = "HOME") String deliveryOption,
                                RedirectAttributes ra) {
        Product p = productRepository.findById(id).filter(Product::isAvailable).orElseThrow();

        CustomerOrder order = new CustomerOrder();
        order.setTrackingNumber("BOL-WA-" + UUID.randomUUID().toString().replace("-","").substring(0,8).toUpperCase());
        order.setCustomerName("Via WhatsApp");
        order.setCustomerPhone("");
        order.setCustomerAddress("");
        order.setDeliveryOption("PICKUP".equals(deliveryOption) ? DeliveryOption.PICKUP : DeliveryOption.HOME);
        order.setTotalAmountCfa(p.getEffectivePriceCfa());
        order.setDeliveryFeeCfa(p.isDeliveryAvailable() ? p.getDeliveryPriceCfa() : 0L);

        OrderLine line = new OrderLine();
        line.setProduct(p);
        line.setQuantity(1);
        line.setUnitPriceCfa(p.getEffectivePriceCfa());
        order.addLine(line);
        orderRepository.save(order);

        String opt = "PICKUP".equals(deliveryOption) ? "Retrait en boutique" : "Livraison à domicile";
        String msg = "Bonjour Bola's 👋\nJe souhaite commander :\n"
                + "📦 Produit : " + p.getName() + "\n"
                + "💰 Prix : " + p.getEffectivePriceCfa() + " CFA\n"
                + "🚚 Option : " + opt + "\n"
                + "📋 N° de suivi : " + order.getTrackingNumber();

        String waUrl = "https://wa.me/" + whatsappNumber
                + "?text=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);
        return "redirect:" + waUrl;
    }
}

