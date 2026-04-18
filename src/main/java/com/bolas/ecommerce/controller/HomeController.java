package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.repository.CategoryRepository;
import com.bolas.ecommerce.repository.CountryRepository;
import com.bolas.ecommerce.repository.ProductRepository;
import com.bolas.ecommerce.repository.ReviewRepository;
import com.bolas.ecommerce.repository.VendorUserRepository;
import com.bolas.ecommerce.model.Product;
import com.bolas.ecommerce.model.VendorStatus;
import com.bolas.ecommerce.model.VendorUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
public class HomeController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final VendorUserRepository vendorUserRepository;
    private final CountryRepository countryRepository;
    private final ReviewRepository reviewRepository;

    public HomeController(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          VendorUserRepository vendorUserRepository,
                          CountryRepository countryRepository,
                          ReviewRepository reviewRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.vendorUserRepository = vendorUserRepository;
        this.countryRepository = countryRepository;
        this.reviewRepository = reviewRepository;
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
            case "GA":
                countryName = "La Marketplace du Gabon";
                countryFlag = "\uD83C\uDDEC\uD83C\uDDE6"; // 🇬🇦
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
        model.addAttribute("featuredProducts", productRepository.findFeaturedForHomepage());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("popularProducts", productRepository.findPopularForHomepage());
        model.addAttribute("countryCount", countryRepository.countByActiveTrue());
        // Bannières PREMIUM
        model.addAttribute("premiumBanners",
                vendorUserRepository.findActivePremiumWithBanner());
        return "index";
    }

    @GetMapping("/products")
    @Transactional(readOnly = true)
    public String products(@RequestParam(required = false) String q,
                           @RequestParam(required = false) Long categoryId,
                           @RequestParam(required = false) Long priceMin,
                           @RequestParam(required = false) Long priceMax,
                           Model model) {
        model.addAttribute("pageTitle", "Produits — BOLA");
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("q", q);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("priceMin", priceMin);
        model.addAttribute("priceMax", priceMax);

        List<?> products;
        if (q != null && !q.isBlank()) {
            products = productRepository.searchByKeyword(q.trim(), categoryId);
        } else if (categoryId != null && priceMin != null && priceMax != null) {
            products = productRepository.findByAvailableTrueAndCategory_IdAndPriceCfaBetween(
                    categoryId, priceMin, priceMax);
        } else if (priceMin != null && priceMax != null) {
            products = productRepository.findByAvailableTrueAndPriceCfaBetween(priceMin, priceMax);
        } else {
            products = productRepository.findAllAvailablePremiumFirst();
        }
        model.addAttribute("products", products);
        return "products";
    }

    @GetMapping("/boutiques")
    @Transactional(readOnly = true)
    public String boutiques(Model model) {
        model.addAttribute("pageTitle", "Boutiques — BOLA");
        try {
            List<VendorUser> activeVendors = vendorUserRepository.findByVendorStatusAndActiveTrue(VendorStatus.ACTIVE);
            model.addAttribute("vendors", activeVendors);
        } catch (Exception e) {
            model.addAttribute("vendors", java.util.List.of());
        }
        return "boutiques";
    }

    @GetMapping("/boutiques/{id}")
    @Transactional(readOnly = true)
    public String boutiqueDetail(@PathVariable Long id, Model model) {
        var vendor = vendorUserRepository.findById(id)
                .filter(v -> v.getVendorStatus() == VendorStatus.ACTIVE && v.isActive())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Boutique introuvable"));
        model.addAttribute("pageTitle", vendor.getDisplayName() + " — BOLA");
        model.addAttribute("vendor", vendor);
        model.addAttribute("products", productRepository.findByVendorAndAvailableTrue(vendor));

        // Avis et note moyenne
        try {
            model.addAttribute("vendorReviews", reviewRepository.findApprovedByVendor(vendor));
            model.addAttribute("avgRating", reviewRepository.averageRatingByVendor(vendor));
            model.addAttribute("reviewCount", reviewRepository.countApprovedByVendor(vendor));
        } catch (Exception e) {
            model.addAttribute("vendorReviews", java.util.List.of());
            model.addAttribute("avgRating", 0.0);
            model.addAttribute("reviewCount", 0L);
        }

        return "boutique-detail";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("pageTitle", "Contact — BOLA");
        return "contact";
    }

    @GetMapping("/products/{id}")
    @Transactional(readOnly = true)
    public String productDetail(@PathVariable Long id, Model model) {
        Product product = productRepository.findByIdWithDetails(id)
                .filter(p -> p.isAvailable()
                        && (p.getVendor() == null
                            || (p.getVendor().isActive()
                                && p.getVendor().getVendorStatus() == VendorStatus.ACTIVE)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit introuvable"));
        model.addAttribute("pageTitle", product.getName() + " — BOLA");
        model.addAttribute("product", product);
        return "product-detail";
    }

    /** Résoit le pays : param URL > cookie > Accept-Language > défaut TG */
    private String resolveCountry(String param, HttpServletRequest request) {
        // 1. Paramètre URL
        if (param != null && !param.isBlank()) {
            String p = param.trim().toUpperCase();
            if ("TG".equals(p) || "CI".equals(p) || "GA".equals(p)) return p;
        }
        // 2. Cookie
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("bolas_country".equals(c.getName())) {
                    String v = c.getValue().toUpperCase();
                    if ("TG".equals(v) || "CI".equals(v) || "GA".equals(v)) return v;
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

