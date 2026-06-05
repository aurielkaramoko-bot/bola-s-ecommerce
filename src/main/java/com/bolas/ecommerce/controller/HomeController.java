package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.dto.ProductFilterDto;
import com.bolas.ecommerce.repository.CategoryRepository;
import com.bolas.ecommerce.repository.CountryRepository;
import com.bolas.ecommerce.repository.ProductRepository;
import com.bolas.ecommerce.repository.ProductSpecification;
import com.bolas.ecommerce.repository.ReviewRepository;
import com.bolas.ecommerce.repository.VendorUserRepository;
import com.bolas.ecommerce.model.Product;
import com.bolas.ecommerce.model.VendorStatus;
import com.bolas.ecommerce.model.VendorUser;
import com.bolas.ecommerce.util.WhatsAppLinkBuilder;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
    private final WhatsAppLinkBuilder whatsAppLinkBuilder;

    @Value("${google.maps.api.key:}")
    private String googleMapsApiKey;

    @Value("${whatsapp.number:}")
    private String shopWhatsapp;

    @Value("${app.base-url:https://bola-marketplace.onrender.com}")
    private String appBaseUrl;

    public HomeController(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          VendorUserRepository vendorUserRepository,
                          CountryRepository countryRepository,
                          ReviewRepository reviewRepository,
                          WhatsAppLinkBuilder whatsAppLinkBuilder) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.vendorUserRepository = vendorUserRepository;
        this.countryRepository = countryRepository;
        this.reviewRepository = reviewRepository;
        this.whatsAppLinkBuilder = whatsAppLinkBuilder;
    }

    @GetMapping("/")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
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
        model.addAttribute("categories", categoryRepository.findByParentIdIsNullAndActiveTrueOrderByNameAsc());
        model.addAttribute("popularProducts", productRepository.findPopularForHomepage());
        model.addAttribute("countryCount", countryRepository.countByActiveTrue());
        // Bannières PREMIUM (pleine largeur si 1 seul, grille 2 colonnes si plusieurs)
        model.addAttribute("premiumBanners",
                vendorUserRepository.findActivePremiumWithBanner());
        // Bannières PRO (section secondaire)
        model.addAttribute("proBanners",
                vendorUserRepository.findActiveProWithBanner());
        // Compteurs dynamiques — requêtes COUNT optimisées (pas de chargement en mémoire)
        model.addAttribute("activeVendorCount",
                vendorUserRepository.countByVendorStatusAndActiveTrue(VendorStatus.ACTIVE));
        model.addAttribute("activeProductCount",
                productRepository.countByAvailableTrue());
        // Produits en tendance (max 8 pour la homepage)
        var trendProducts = productRepository.findCurrentlyTrendingAvailable();
        model.addAttribute("trendProducts", trendProducts);
        return "index";
    }

    @GetMapping("/products")
    @Transactional(readOnly = true)
    public String products(@ModelAttribute ProductFilterDto filter, Model model) {
        model.addAttribute("pageTitle", "Produits — BOLA");
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("filter", filter);

        // Tri
        Sort sort = switch (filter.getSortBy() != null ? filter.getSortBy() : "") {
            case "price_asc"  -> Sort.by(Sort.Direction.ASC,  "priceCfa");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "priceCfa");
            case "newest"    -> Sort.by(Sort.Direction.DESC, "id");
            default          -> Sort.by(Sort.Direction.DESC, "id"); // newest par défaut
        };

        List<Product> products;
        if (filter.hasActiveFilter()) {
            products = productRepository.findAll(ProductSpecification.of(filter), sort);
        } else {
            products = productRepository.findAllAvailablePremiumFirst();
        }
        model.addAttribute("products", products);
        model.addAttribute("totalCount", products.size());
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
    public String boutiqueDetail(@PathVariable Long id, Model model,
                                  jakarta.servlet.http.HttpSession session) {
        var vendor = vendorUserRepository.findById(id)
                .filter(v -> v.getVendorStatus() == VendorStatus.ACTIVE && v.isActive())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Boutique introuvable"));
        model.addAttribute("pageTitle", vendor.getDisplayName() + " — BOLA");
        model.addAttribute("vendor", vendor);
        model.addAttribute("products", productRepository.findByVendorAndAvailableTrue(vendor));

        // Client connecté (pour formulaire avis)
        model.addAttribute("connectedCustomer", session.getAttribute("BOLAS_CUSTOMER"));

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

        // Localisation
        model.addAttribute("googleMapsApiKey", googleMapsApiKey);

        return "boutique-detail";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("pageTitle", "Contact — BOLA");
        return "contact";
    }

    @GetMapping("/trends")
    @Transactional(readOnly = true)
    public String trends(Model model) {
        model.addAttribute("pageTitle", "Tendances du moment — BOLA");
        // Produits marqués manuellement comme Trend (non expirés)
        var now = java.time.LocalDateTime.now();
        List<Product> trendProducts = productRepository.findCurrentlyTrendingAvailable();
        model.addAttribute("trendProducts", trendProducts);
        return "trends";
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

        // Lien WhatsApp pré-rempli via le bean (instruction GPS incluse)
        String waUrl = whatsAppLinkBuilder.productOrderUrl(product, "");
        model.addAttribute("waOrderUrl", waUrl);

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

