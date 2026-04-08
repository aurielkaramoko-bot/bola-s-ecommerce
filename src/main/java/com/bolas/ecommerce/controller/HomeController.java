package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.repository.CategoryRepository;
import com.bolas.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public HomeController(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("pageTitle", "BOLA — La Marketplace du Togo");
        model.addAttribute("featuredProducts", productRepository.findByAvailableTrueAndFeaturedTrue());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("popularProducts", productRepository.findTop6ByAvailableTrueOrderByFeaturedDescIdDesc());
        return "index";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("pageTitle", "Contact — BOLA");
        return "contact";
    }
}
