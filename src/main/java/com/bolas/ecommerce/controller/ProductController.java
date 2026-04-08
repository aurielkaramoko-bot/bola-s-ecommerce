package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.model.Product;
import com.bolas.ecommerce.repository.CategoryRepository;
import com.bolas.ecommerce.repository.ProductRepository;
import com.bolas.ecommerce.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProductController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;

    private final String whatsappNumber;
    private final String shopPhone;

    public ProductController(ProductRepository productRepository,
                             CategoryRepository categoryRepository,
                             ReviewRepository reviewRepository,
                             @Value("${whatsapp.number}") String whatsappNumber,
                             @Value("${bolas.shop.phone}") String shopPhone) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.reviewRepository = reviewRepository;
        this.whatsappNumber = whatsappNumber;
        this.shopPhone = shopPhone;
    }

    @GetMapping("/products")
    public String list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long priceMin,
            @RequestParam(required = false) Long priceMax,
            Model model) {
        // Validation et clamp des prix pour éviter les valeurs aberrantes
        long min = (priceMin != null && priceMin >= 0) ? priceMin : 0L;
        long max = (priceMax != null && priceMax >= 0 && priceMax <= 100_000_000L)
                   ? priceMax : Long.MAX_VALUE;
        if (min > max) { min = 0L; max = Long.MAX_VALUE; }

        var products = categoryId != null
                ? productRepository.findByAvailableTrueAndCategory_IdAndPriceCfaBetween(categoryId, min, max)
                : productRepository.findByAvailableTrueAndPriceCfaBetween(min, max);

        model.addAttribute("pageTitle", "Produits — BOLA");
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("priceMin", priceMin);
        model.addAttribute("priceMax", priceMax);
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
}

