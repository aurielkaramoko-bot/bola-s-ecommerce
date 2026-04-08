package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.repository.CategoryRepository;
import com.bolas.ecommerce.repository.ProductRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public HomeController(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/")
    public String home(@RequestParam(value = "country", required = false) String countryParam,
                       HttpServletRequest request,
                       HttpServletResponse response,
                       Model model) {

        String country = resolveCountry(countryParam, request);

        // Persister le choix en cookie (30 jours)
        if (countryParam != null && !countryParam.isBlank()) {
            Cookie c = new Cookie("bolas_country", country);
            c.setMaxAge(60 * 60 * 24 * 30);
            c.setPath("/");
            response.addCookie(c);
        }

        String countryName;
        String countryFlag;
        switch (country) {
            case "CI":
                countryName = "La Marketplace de la Côte d'Ivoire";
                countryFlag = "\uD83C\uDDE8\uD83C\uDDEE"; // 🇨🇮
                break;
            case "TG":
                countryName = "La Marketplace du Togo";
                countryFlag = "\uD83C\uDDF9\uD83C\uDDEC"; // 🇹🇬
                break;
            default:
                countryName = "La Marketplace d'Afrique de l'Ouest";
                countryFlag = "\uD83C\uDF0D"; // 🌍
                break;
        }

        model.addAttribute("pageTitle", "BOLA — " + countryName);
        model.addAttribute("countryCode", country);
        model.addAttribute("countryName", countryName);
        model.addAttribute("countryFlag", countryFlag);
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

    /** Résoit le pays : param URL > cookie > Accept-Language > défaut TG */
    private String resolveCountry(String param, HttpServletRequest request) {
        // 1. Paramètre URL
        if (param != null && !param.isBlank()) {
            String p = param.trim().toUpperCase();
            if ("TG".equals(p) || "CI".equals(p)) return p;
        }
        // 2. Cookie
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("bolas_country".equals(c.getName())) {
                    String v = c.getValue().toUpperCase();
                    if ("TG".equals(v) || "CI".equals(v)) return v;
                }
            }
        }
        // 3. Accept-Language (fr-CI → CI)
        String lang = request.getHeader("Accept-Language");
        if (lang != null) {
            String lower = lang.toLowerCase();
            if (lower.contains("ci") || lower.contains("côte") || lower.contains("ivory")) return "CI";
        }
        // 4. Défaut
        return "TG";
    }
}

