package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.model.*;
import com.bolas.ecommerce.repository.*;
import com.bolas.ecommerce.service.IdDocumentVerificationService;
import com.bolas.ecommerce.service.InputSanitizerService;
import com.bolas.ecommerce.service.ImageUploadService;
import com.bolas.ecommerce.service.MetaWhatsAppService;
import com.bolas.ecommerce.service.SessionCounterService;
import com.bolas.ecommerce.service.WhatsAppNotificationService;
import com.bolas.ecommerce.service.VendorStatsService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vendor")
public class VendorController {

    private static final Logger log = LoggerFactory.getLogger(VendorController.class);
    private static final String SESSION_KEY   = "BOLAS_VENDOR";
    private static final int    GRATUIT_LIMIT = 10;

    private final CustomerOrderRepository   orderRepository;
    private final VendorUserRepository       vendorUserRepository;
    private final ProductRepository          productRepository;
    private final CategoryRepository         categoryRepository;
    private final VendorCategoryRepository   vendorCategoryRepository;
    private final ChatMessageRepository      chatMessageRepository;
    private final PasswordEncoder            passwordEncoder;
    private final ImageUploadService         imageUploadService;
    private final WhatsAppNotificationService whatsAppService;
    private final InputSanitizerService      sanitizer;
    private final IdDocumentVerificationService idVerificationService;
    private final CourierApplicationRepository courierApplicationRepository;
    private final MetaWhatsAppService        metaWhatsApp;
    private final LoyaltyCardRepository      loyaltyCardRepository;
    private final SessionCounterService      sessionCounter;
    private final com.bolas.ecommerce.service.PackPricingService packPricingService;
    private final com.bolas.ecommerce.service.OrderFlowService   orderFlowService;
    private final VendorStatsService          vendorStatsService;
    private final ReviewRepository            reviewRepository;

    public VendorController(CustomerOrderRepository orderRepository,
                            VendorUserRepository vendorUserRepository,
                            ProductRepository productRepository,
                            CategoryRepository categoryRepository,
                            VendorCategoryRepository vendorCategoryRepository,
                            ChatMessageRepository chatMessageRepository,
                            PasswordEncoder passwordEncoder,
                            ImageUploadService imageUploadService,
                            WhatsAppNotificationService whatsAppService,
                            InputSanitizerService sanitizer,
                            IdDocumentVerificationService idVerificationService,
                            CourierApplicationRepository courierApplicationRepository,
                            MetaWhatsAppService metaWhatsApp,
                            LoyaltyCardRepository loyaltyCardRepository,
                            SessionCounterService sessionCounter,
                            com.bolas.ecommerce.service.PackPricingService packPricingService,
                            com.bolas.ecommerce.service.OrderFlowService orderFlowService,
                            VendorStatsService vendorStatsService,
                            ReviewRepository reviewRepository) {
        this.orderRepository               = orderRepository;
        this.vendorUserRepository          = vendorUserRepository;
        this.productRepository             = productRepository;
        this.categoryRepository            = categoryRepository;
        this.vendorCategoryRepository      = vendorCategoryRepository;
        this.chatMessageRepository         = chatMessageRepository;
        this.passwordEncoder               = passwordEncoder;
        this.imageUploadService            = imageUploadService;
        this.whatsAppService               = whatsAppService;
        this.sanitizer                     = sanitizer;
        this.idVerificationService         = idVerificationService;
        this.courierApplicationRepository  = courierApplicationRepository;
        this.metaWhatsApp                  = metaWhatsApp;
        this.loyaltyCardRepository         = loyaltyCardRepository;
        this.sessionCounter                = sessionCounter;
        this.packPricingService            = packPricingService;
        this.orderFlowService              = orderFlowService;
        this.vendorStatsService            = vendorStatsService;
        this.reviewRepository              = reviewRepository;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private VendorUser currentVendor(HttpSession session) {
        Object obj = session.getAttribute(SESSION_KEY);
        if (!(obj instanceof VendorUser v)) return null;
        // Revalider en base à chaque requête — détecte suspension immédiate
        return vendorUserRepository.findById(v.getId())
                .filter(fresh -> fresh.isActive() && fresh.getVendorStatus() == VendorStatus.ACTIVE)
                .orElse(null);
    }

    private String requireVendor(HttpSession session) {
        VendorUser v = currentVendor(session);
        if (v == null) {
            session.removeAttribute(SESSION_KEY); // nettoie la session si suspendu
            return "redirect:/vendor/login";
        }
        return null;
    }

    /** Catégories autorisées pour ce vendeur — strictement celles assignées par l'admin */
    private List<Category> allowedCategories(VendorUser vendor) {
        return vendorCategoryRepository.findCategoriesByVendor(vendor);
    }

    // ─── Authentification ─────────────────────────────────────────────────────

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        if (currentVendor(session) != null) return "redirect:/vendor/dashboard";
        model.addAttribute("pageTitle", "Espace Vendeur — BOLA");
        return "vendor/login";
    }

    @PostMapping("/login-process")
    public String loginProcess(@RequestParam String username,
                               @RequestParam String password,
                               HttpSession session,
                               jakarta.servlet.http.HttpServletRequest request,
                               RedirectAttributes ra) {
        Optional<VendorUser> opt = vendorUserRepository.findByUsername(username.trim());
        if (opt.isEmpty() || !passwordEncoder.matches(password, opt.get().getPasswordHash())) {
            ra.addFlashAttribute("flashError", "Identifiant ou mot de passe incorrect.");
            return "redirect:/vendor/login";
        }
        VendorUser v = opt.get();
        if (!v.isActive()) {
            if (v.getVendorStatus() == VendorStatus.PENDING) {
                ra.addFlashAttribute("flashError",
                        "Votre demande d'ouverture de boutique est en cours de validation. Nous vous contacterons sous 24h.");
            } else {
                ra.addFlashAttribute("flashError",
                        "Votre compte vendeur a été suspendu. Contactez l'administration.");
            }
            return "redirect:/vendor/login";
        }
        // Sécurité session fixation : régénère l'ID de session sans perdre les données
        request.changeSessionId();
        session.setAttribute(SESSION_KEY, v);
        sessionCounter.markVendorSession(session); // compteur de vendeurs connectés
        return "redirect:/vendor/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        sessionCounter.unmarkVendorSession(session);
        session.removeAttribute(SESSION_KEY);
        return "redirect:/vendor/login";
    }

    // ─── Inscription publique ─────────────────────────────────────────────────

    @GetMapping("/register")
    @Transactional(readOnly = true)
    public String registerPage(HttpSession session, Model model) {
        if (currentVendor(session) != null) return "redirect:/vendor/dashboard";
        model.addAttribute("pageTitle", "Ouvrir ma boutique — BOLA");
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("gratuitPrice",  packPricingService.getGratuitPrice());
        model.addAttribute("proLocalPrice", packPricingService.getProLocalPrice());
        model.addAttribute("proPrice",      packPricingService.getProPrice());
        model.addAttribute("premiumPrice",  packPricingService.getPremiumPrice());
        return "vendor/register";
    }

    @PostMapping("/register")
    public String registerSubmit(@RequestParam String shopName,
                                 @RequestParam String username,
                                 @RequestParam String phone,
                                 @RequestParam String email,
                                 @RequestParam String password,
                                 @RequestParam(required = false) String shopDescription,
                                 @RequestParam(required = false) List<Long> categoryIds,
                                 @RequestParam(required = false) String requestedNiche,
                                 @RequestParam(value = "selectedPlan", required = false, defaultValue = "GRATUIT") String selectedPlan,
                                 @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
                                 @RequestParam(value = "idDocFile", required = false) MultipartFile idDocFile,
                                 RedirectAttributes ra) {

        // ── Sanitisation des champs texte ──────────────────────────────────────
        String cleanShopName    = sanitizer.sanitizeText(shopName);
        String cleanUsername    = sanitizer.sanitizeText(username);
        String cleanPhone       = sanitizer.sanitizeText(phone);
        String cleanEmail       = sanitizer.sanitizeText(email);
        String cleanDesc        = sanitizer.sanitizeText(shopDescription);
        String cleanNiche       = sanitizer.sanitizeText(requestedNiche);

        // ── Validations ────────────────────────────────────────────────────────
        if (cleanShopName == null || cleanShopName.isBlank()) {
            ra.addFlashAttribute("flashError", "Le nom de la boutique est obligatoire.");
            return "redirect:/vendor/register";
        }
        if (cleanUsername == null || cleanUsername.isBlank()) {
            ra.addFlashAttribute("flashError", "L'identifiant est obligatoire.");
            return "redirect:/vendor/register";
        }
        if (!cleanUsername.matches("[a-zA-Z0-9._-]+")) {
            ra.addFlashAttribute("flashError", "L'identifiant ne peut contenir que des lettres, chiffres, tirets et points.");
            return "redirect:/vendor/register";
        }
        if (cleanEmail == null || cleanEmail.isBlank()) {
            ra.addFlashAttribute("flashError", "L'email est obligatoire.");
            return "redirect:/vendor/register";
        }
        if (!cleanEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            ra.addFlashAttribute("flashError", "L'email n'est pas valide.");
            return "redirect:/vendor/register";
        }
        if (password == null || password.length() < 8) {
            ra.addFlashAttribute("flashError", "Le mot de passe doit faire au moins 8 caractères.");
            return "redirect:/vendor/register";
        }
        // Photo de boutique obligatoire
        if (logoFile == null || logoFile.isEmpty()) {
            ra.addFlashAttribute("flashError", "La photo de boutique / logo est obligatoire.");
            return "redirect:/vendor/register";
        }
        // Pièce d'identité obligatoire
        if (idDocFile == null || idDocFile.isEmpty()) {
            ra.addFlashAttribute("flashError", "La pièce d'identité est obligatoire pour valider votre boutique.");
            return "redirect:/vendor/register";
        }
        if (vendorUserRepository.findByUsername(cleanUsername).isPresent()) {
            ra.addFlashAttribute("flashError", "Ce nom d'utilisateur est déjà pris.");
            return "redirect:/vendor/register";
        }
        if (vendorUserRepository.findByEmail(cleanEmail).isPresent()) {
            ra.addFlashAttribute("flashError", "Cet email est déjà utilisé.");
            return "redirect:/vendor/register";
        }

        // ── Plan choisi ────────────────────────────────────────────────────────
        VendorPlan plan;
        try {
            plan = VendorPlan.valueOf(selectedPlan.toUpperCase());
        } catch (IllegalArgumentException e) {
            plan = VendorPlan.GRATUIT;
        }

        // ── Upload photo boutique (obligatoire, magic bytes vérifiés) ──────────
        String logoUrl = null;
        try {
            logoUrl = imageUploadService.store(logoFile);
        } catch (IllegalArgumentException | IOException e) {
            ra.addFlashAttribute("flashError", "Photo de boutique invalide : " + e.getMessage());
            return "redirect:/vendor/register";
        }

        // ── Upload pièce d'identité (obligatoire, magic bytes vérifiés) ────────
        String idDocUrl = null;
        try {
            idDocUrl = imageUploadService.store(idDocFile);
        } catch (IllegalArgumentException | IOException e) {
            ra.addFlashAttribute("flashError", "Pièce d'identité invalide : " + e.getMessage());
            return "redirect:/vendor/register";
        }

        // ── Scan Vision API — vérification que c'est bien un document d'identité ──
        Boolean idVerified = idVerificationService.verify(idDocFile);
        if (Boolean.FALSE.equals(idVerified)) {
            // Vision a analysé l'image et n'a trouvé aucun mot-clé de document officiel
            ra.addFlashAttribute("flashError",
                    "La pièce d'identité soumise ne semble pas être un document officiel (CNI, passeport, permis). " +
                    "Veuillez uploader une photo nette de votre document.");
            return "redirect:/vendor/register";
        }
        // idVerified == null → clé Vision non configurée, on laisse passer (admin vérifie manuellement)

        // ── Création du vendeur ────────────────────────────────────────────────
        VendorUser v = new VendorUser();
        v.setUsername(cleanUsername);
        v.setShopName(cleanShopName);
        v.setPhone(cleanPhone);
        v.setEmail(cleanEmail);
        v.setShopDescription(cleanDesc);
        v.setPasswordHash(passwordEncoder.encode(password));
        v.setActive(false);
        v.setVendorStatus(VendorStatus.PENDING);
        v.setPlan(plan);
        v.setLogoUrl(logoUrl);
        v.setIdDocumentUrl(idDocUrl);
        v.setIdDocVerified(idVerified);
        if (cleanNiche != null && !cleanNiche.isBlank()) {
            v.setRequestedNiche(cleanNiche);
        }

        vendorUserRepository.save(v);

        // ── Catégories ─────────────────────────────────────────────────────────
        String catNames = "";
        if (categoryIds != null && !categoryIds.isEmpty()) {
            for (Long catId : categoryIds) {
                categoryRepository.findById(catId).ifPresent(cat ->
                        vendorCategoryRepository.save(new VendorCategory(v, cat)));
            }
            catNames = categoryIds.stream()
                    .map(id -> categoryRepository.findById(id).map(Category::getName).orElse("?"))
                    .collect(Collectors.joining(", "));
        }

        // Envoi automatique via Meta Cloud API (si configuré)
        try {
            String adminNotifMsg = "🆕 Nouvelle demande de boutique sur BOLA !\n\n"
                    + "🏪 Boutique : " + v.getShopName() + "\n"
                    + "👤 Identifiant : " + v.getUsername() + "\n"
                    + "� Email : " + v.getEmail() + "\n"
                    + "📞 Téléphone : " + v.getPhone() + "\n"
                    + (catNames.isBlank() ? "" : "🏷️ Catégories : " + catNames + "\n")
                    + (v.getRequestedNiche() != null ? "💡 Niche demandée : " + v.getRequestedNiche() + "\n" : "")
                    + "📋 Plan : " + v.getPlan().name() + "\n"
                    + "\n→ Validez depuis l'admin BOLA";
            metaWhatsApp.sendText(whatsAppService.getAdminWhatsApp(), adminNotifMsg);
            log.info("✅ Notification WhatsApp inscription vendeur envoyée pour {} ({})", v.getShopName(), v.getUsername());
        } catch (Exception e) {
            log.warn("⚠️ Notification WhatsApp inscription vendeur échouée (inscription sauvegardée quand même) pour {}: {}", 
                    v.getUsername(), e.getMessage());
        }

        ra.addFlashAttribute("flashOk",
                "✅ Demande envoyée ! Notre équipe validera votre boutique sous 24h.");
        return "redirect:/vendor/register";    }

    // ─── Dashboard vendeur ────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public String dashboard(HttpSession session, Model model, HttpServletRequest request) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;

        VendorUser vendor = currentVendor(session);
        vendor = vendorUserRepository.findById(vendor.getId()).orElse(vendor);
        session.setAttribute(SESSION_KEY, vendor);

        long productCount   = productRepository.countByVendor(vendor);
        // Compter uniquement les commandes de CE vendeur
        long pendingOrders  = orderRepository.countByVendorAndStatus(vendor, OrderStatus.PENDING);
        long confirmedOrders = orderRepository.countByVendorAndStatus(vendor, OrderStatus.CONFIRMED);

        String scheme = request.getHeader("X-Forwarded-Proto") != null
                ? request.getHeader("X-Forwarded-Proto") : request.getScheme();
        String shopBaseUrl = scheme + "://" + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort());

        model.addAttribute("pageTitle", "Mon espace vendeur — BOLA");
        model.addAttribute("vendor",    vendor);
        model.addAttribute("productCount",    productCount);
        model.addAttribute("pendingOrders",   pendingOrders);
        model.addAttribute("confirmedOrders", confirmedOrders);
        model.addAttribute("gratuitLimit",    GRATUIT_LIMIT);
        model.addAttribute("allowedCategories", allowedCategories(vendor));
        model.addAttribute("shopBaseUrl", shopBaseUrl);
        model.addAttribute("products",
                productRepository.findByVendor(vendor).stream()
                        .limit(5).toList());

        // Stats basiques si PRO/PREMIUM
        if (vendor.canViewStats()) {
            try {
                model.addAttribute("stats", vendorStatsService.getBasicStats(vendor));
            } catch (Exception e) {
                log.warn("Erreur chargement stats vendeur {}: {}", vendor.getId(), e.getMessage());
            }
        }

        // Livreur assigné
        if (vendor.getAssignedCourierId() != null) {
            courierApplicationRepository.findById(vendor.getAssignedCourierId())
                    .ifPresent(c -> model.addAttribute("assignedCourier", c));
        }

        // Messages non lus
        long unreadMessages = chatMessageRepository.countByVendorAndReadByVendorFalseAndSenderType(vendor, "CUSTOMER");
        model.addAttribute("unreadMessages", unreadMessages);

        // Avis
        try {
            model.addAttribute("avgRating", reviewRepository.averageRatingByVendor(vendor));
            model.addAttribute("reviewCount", reviewRepository.countApprovedByVendor(vendor));
        } catch (Exception e) {
            model.addAttribute("avgRating", 0.0);
            model.addAttribute("reviewCount", 0L);
        }

        return "vendor/dashboard";
    }

    // ─── Commandes vendeur ────────────────────────────────────────────────────

    @GetMapping("/orders")
    @Transactional(readOnly = true)
    public String orders(HttpSession session, Model model) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;

        VendorUser vendor = currentVendor(session);

        // GRATUIT : voit les commandes mais boutons grisés avec message upgrade
        if (vendor.getPlan() == VendorPlan.GRATUIT) {
            // Montrer les commandes en lecture seule pour frustrer gentiment
            var allVendorOrders = orderRepository.findByVendorOrderByCreatedAtDesc(vendor);
            model.addAttribute("pageTitle", "Mes commandes — BOLA Vendeur");
            model.addAttribute("vendor", vendor);
            model.addAttribute("readOnlyMode", true);
            model.addAttribute("upgradeMessage",
                    "Passez en PRO pour gérer vos commandes vous-même ! " +
                    "Actuellement, c'est l'admin BOLA qui gère vos commandes.");
            model.addAttribute("toProcess", allVendorOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.PENDING || o.getStatus() == OrderStatus.CONFIRMED)
                    .toList());
            model.addAttribute("done", allVendorOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.READY || o.getStatus() == OrderStatus.DELIVERED)
                    .toList());
            model.addAttribute("approvedCouriers", List.of());
            return "vendor/orders";
        }

        // PRO / PREMIUM : gère ses commandes
        List<CustomerOrder> toProcess =
                orderRepository.findByVendorAndStatusInOrderByCreatedAtDesc(vendor,
                        List.of(OrderStatus.CONFIRMED));
        List<CustomerOrder> done =
                orderRepository.findByVendorAndStatusInOrderByCreatedAtDesc(vendor,
                        List.of(OrderStatus.READY));

        // Livreurs approuvés proposés par ce vendeur + livreur assigné par admin
        var approvedCouriers = courierApplicationRepository.findByVendorOrderBySubmittedAtDesc(vendor)
                .stream()
                .filter(a -> a.getStatus() == CourierApplicationStatus.APPROVED)
                .collect(Collectors.toList());
        // Ajouter le livreur assigné par admin s'il n'est pas déjà dans la liste
        if (vendor.getAssignedCourierId() != null) {
            courierApplicationRepository.findById(vendor.getAssignedCourierId())
                    .filter(c -> c.getStatus() == CourierApplicationStatus.APPROVED)
                    .filter(c -> approvedCouriers.stream().noneMatch(a -> a.getId().equals(c.getId())))
                    .ifPresent(approvedCouriers::add);
        }

        model.addAttribute("pageTitle", "Mes commandes — BOLA Vendeur");
        model.addAttribute("vendor",    vendor);
        model.addAttribute("readOnlyMode", false);
        model.addAttribute("toProcess", toProcess);
        model.addAttribute("done",      done);
        model.addAttribute("approvedCouriers", approvedCouriers);
        return "vendor/orders";
    }

    @PostMapping("/orders/{id}/ready")
    public String markReady(@PathVariable Long id,
                            HttpSession session,
                            RedirectAttributes ra) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;

        VendorUser vendor = currentVendor(session);
        CustomerOrder order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            ra.addFlashAttribute("flashError", "Commande introuvable.");
            return "redirect:/vendor/orders";
        }

        // Vérifier que cette commande appartient bien à ce vendeur
        boolean belongs = order.getVendor() != null && order.getVendor().getId().equals(vendor.getId());
        if (!belongs) {
            // Fallback : vérifier via les lignes (commandes créées avant la migration)
            belongs = order.getLines().stream()
                    .anyMatch(l -> l.getProduct() != null
                            && l.getProduct().getVendor() != null
                            && l.getProduct().getVendor().getId().equals(vendor.getId()));
        }
        if (!belongs) {
            ra.addFlashAttribute("flashError", "Accès refusé à cette commande.");
            return "redirect:/vendor/orders";
        }

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            ra.addFlashAttribute("flashError", "Cette commande ne peut pas être marquée prête.");
            return "redirect:/vendor/orders";
        }

        String appBaseUrl = "https://bola-s-ecommerce.onrender.com";
        String waAdminLink = orderFlowService.markReady(order, appBaseUrl);
        ra.addFlashAttribute("flashOk", "Commande marquée comme prête !");
        ra.addFlashAttribute("waAdminLink", waAdminLink);
        return "redirect:/vendor/orders";
    }

    @PostMapping("/orders/{id}/assign-courier")
    @org.springframework.transaction.annotation.Transactional
    public String assignCourierToOrder(@PathVariable Long id,
                                       @RequestParam Long courierId,
                                       HttpSession session,
                                       RedirectAttributes ra) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;

        VendorUser vendor = currentVendor(session);
        if (vendor.getPlan() == VendorPlan.GRATUIT) {
            ra.addFlashAttribute("flashError", "Fonctionnalité réservée aux packs payants.");
            return "redirect:/vendor/orders";
        }

        courierApplicationRepository.findById(courierId).ifPresent(courier -> {
            orderRepository.findById(id).ifPresent(order -> {
                order.setAssignedCourierName(courier.getCourierName());
                order.setAssignedCourierPhone(courier.getCourierPhone());
                orderRepository.save(order);
                log.info("✅ Livreur {} assigné à commande {} par vendeur {}", courier.getCourierName(), id, vendor.getDisplayName());
            });
        });
        ra.addFlashAttribute("flashOk", "Livreur assigné à la commande.");
        return "redirect:/vendor/orders";
    }

    // ─── Produits vendeur ─────────────────────────────────────────────────────

    @GetMapping("/products")
    @Transactional(readOnly = true)
    public String myProducts(HttpSession session, Model model) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;

        VendorUser vendor = currentVendor(session);
        List<Product> products = productRepository.findByVendor(vendor);
        long count = products.size();

        model.addAttribute("pageTitle",   "Mes produits — BOLA");
        model.addAttribute("vendor",      vendor);
        model.addAttribute("products",    products);
        model.addAttribute("productCount", count);
        model.addAttribute("gratuitLimit", GRATUIT_LIMIT);
        model.addAttribute("limitReached",
                vendor.getPlan() == VendorPlan.GRATUIT && count >= GRATUIT_LIMIT);
        return "vendor/products";
    }

    @GetMapping("/products/add")
    @Transactional(readOnly = true)
    public String addProductForm(HttpSession session, Model model) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;

        VendorUser vendor = currentVendor(session);
        if (vendor.getPlan() == VendorPlan.GRATUIT &&
                productRepository.countByVendor(vendor) >= GRATUIT_LIMIT) {
            model.addAttribute("pageTitle", "Mes produits — BOLA");
            model.addAttribute("vendor",   vendor);
            model.addAttribute("flashError",
                    "Limite atteinte (10 produits max en Pack Débutant). Passez au Pack Pro.");
            model.addAttribute("products",  productRepository.findByVendor(vendor));
            model.addAttribute("gratuitLimit", GRATUIT_LIMIT);
            model.addAttribute("limitReached", true);
            return "vendor/products";
        }

        Product p = new Product();
        p.setAvailable(true);
        p.setDeliveryAvailable(true);
        p.setFeatured(false);
        p.setDeliveryPriceCfa(0L);

        model.addAttribute("pageTitle",  "Ajouter un produit — BOLA");
        model.addAttribute("vendor",     vendor);
        model.addAttribute("product",    p);
        // Seules les catégories autorisées du vendeur
        model.addAttribute("categories", allowedCategories(vendor));
        model.addAttribute("isEdit",     false);
        return "vendor/product-form";
    }

    @PostMapping("/products/add")
    public String saveProduct(@RequestParam String name,
                              @RequestParam(required = false) String description,
                              @RequestParam Long priceCfa,
                              @RequestParam(required = false) Long promoPriceCfa,
                              @RequestParam(required = false) String promoLabel,
                              @RequestParam Long categoryId,
                              @RequestParam(required = false) String imageUrl,
                              @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                              @RequestParam(value = "videoFile", required = false) MultipartFile videoFile,
                              @RequestParam(defaultValue = "true") boolean available,
                              @RequestParam(defaultValue = "true") boolean deliveryAvailable,
                              @RequestParam(defaultValue = "0") long deliveryPriceCfa,
                              @RequestParam(defaultValue = "false") boolean limitedStock,
                              HttpSession session,
                              RedirectAttributes ra) {

        String redirect = requireVendor(session);
        if (redirect != null) return redirect;

        VendorUser vendor = currentVendor(session);

        if (vendor.getPlan() == VendorPlan.GRATUIT &&
                productRepository.countByVendor(vendor) >= GRATUIT_LIMIT) {
            ra.addFlashAttribute("flashError",
                    "Limite de 10 produits atteinte. Passez au Pack Pro.");
            return "redirect:/vendor/products";
        }

        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category == null) {
            ra.addFlashAttribute("flashError", "Catégorie invalide.");
            return "redirect:/vendor/products/add";
        }

        // Vérifier strictement que le vendeur a accès à cette catégorie
        List<Category> allowed = allowedCategories(vendor);
        if (allowed.stream().noneMatch(c -> c.getId().equals(categoryId))) {
            ra.addFlashAttribute("flashError",
                    "Vous n'avez pas accès à cette catégorie. Envoyez une demande à l'admin depuis le formulaire d'ajout de produit.");
            return "redirect:/vendor/products/add";
        }

        Product p = new Product();
        p.setName(name.trim());
        p.setDescription(description);
        p.setPriceCfa(priceCfa);
        p.setPromoPriceCfa(promoPriceCfa != null && promoPriceCfa > 0 ? promoPriceCfa : null);
        p.setCategory(category);
        p.setAvailable(available);
        p.setDeliveryAvailable(deliveryAvailable);
        p.setDeliveryPriceCfa(deliveryPriceCfa);
        p.setFeatured(false);
        p.setVendor(vendor);
        p.setLimitedStock(limitedStock);
        // promoLabel réservé aux plans PRO et PREMIUM
        if ((vendor.getPlan() == VendorPlan.PRO || vendor.getPlan() == VendorPlan.PRO_LOCAL
                || vendor.getPlan() == VendorPlan.PREMIUM)
                && promoLabel != null && !promoLabel.isBlank()) {
            p.setPromoLabel(promoLabel.trim());
        }

        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                p.setImageUrl(imageUploadService.store(imageFile));
            } else if (imageUrl != null && !imageUrl.isBlank()) {
                p.setImageUrl(imageUrl.trim());
            }
            if (videoFile != null && !videoFile.isEmpty()) {
                p.setVideoUrl(imageUploadService.storeVideo(videoFile));
            }
        } catch (IllegalArgumentException | IOException e) {
            ra.addFlashAttribute("flashError", "Échec upload : " + e.getMessage());
            return "redirect:/vendor/products/add";
        }

        productRepository.save(p);
        ra.addFlashAttribute("flashOk", "Produit \"" + p.getName() + "\" ajouté avec succès !");
        return "redirect:/vendor/products";
    }

    @GetMapping("/products/{id}/edit")
    @Transactional(readOnly = true)
    public String editProductForm(@PathVariable Long id,
                                  HttpSession session,
                                  Model model,
                                  RedirectAttributes ra) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;

        VendorUser vendor = currentVendor(session);
        Product p = productRepository.findById(id).orElse(null);
        if (p == null || p.getVendor() == null || !p.getVendor().getId().equals(vendor.getId())) {
            ra.addFlashAttribute("flashError", "Produit introuvable ou accès refusé.");
            return "redirect:/vendor/products";
        }

        model.addAttribute("pageTitle",  "Modifier le produit — BOLA");
        model.addAttribute("vendor",     vendor);
        model.addAttribute("product",    p);
        model.addAttribute("categories", allowedCategories(vendor));
        model.addAttribute("isEdit",     true);
        return "vendor/product-form";
    }

    @PostMapping("/products/{id}/edit")
    public String updateProduct(@PathVariable Long id,
                                @RequestParam String name,
                                @RequestParam(required = false) String description,
                                @RequestParam Long priceCfa,
                                @RequestParam(required = false) Long promoPriceCfa,
                                @RequestParam(required = false) String promoLabel,
                                @RequestParam Long categoryId,
                                @RequestParam(required = false) String imageUrl,
                                @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                @RequestParam(defaultValue = "true") boolean available,
                                @RequestParam(defaultValue = "true") boolean deliveryAvailable,
                                @RequestParam(defaultValue = "0") long deliveryPriceCfa,
                                @RequestParam(defaultValue = "false") boolean limitedStock,
                                HttpSession session,
                                RedirectAttributes ra) {

        String redirect = requireVendor(session);
        if (redirect != null) return redirect;

        VendorUser vendor = currentVendor(session);
        Product p = productRepository.findById(id).orElse(null);
        if (p == null || p.getVendor() == null || !p.getVendor().getId().equals(vendor.getId())) {
            ra.addFlashAttribute("flashError", "Produit introuvable ou accès refusé.");
            return "redirect:/vendor/products";
        }

        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category == null) {
            ra.addFlashAttribute("flashError", "Catégorie invalide.");
            return "redirect:/vendor/products/" + id + "/edit";
        }

        // Vérifier strictement que le vendeur a accès à cette catégorie
        List<Category> allowedEdit = allowedCategories(vendor);
        if (allowedEdit.stream().noneMatch(c -> c.getId().equals(categoryId))) {
            ra.addFlashAttribute("flashError", "Vous n'avez pas accès à cette catégorie.");
            return "redirect:/vendor/products/" + id + "/edit";
        }

        p.setName(name.trim());
        p.setDescription(description);
        p.setPriceCfa(priceCfa);
        p.setPromoPriceCfa(promoPriceCfa != null && promoPriceCfa > 0 ? promoPriceCfa : null);
        p.setCategory(category);
        p.setAvailable(available);
        p.setDeliveryAvailable(deliveryAvailable);
        p.setDeliveryPriceCfa(deliveryPriceCfa);
        p.setLimitedStock(limitedStock);
        // promoLabel réservé aux plans PRO et PREMIUM
        if ((vendor.getPlan() == VendorPlan.PRO || vendor.getPlan() == VendorPlan.PRO_LOCAL
                || vendor.getPlan() == VendorPlan.PREMIUM)
                && promoLabel != null && !promoLabel.isBlank()) {
            p.setPromoLabel(promoLabel.trim());
        } else if (promoLabel == null || promoLabel.isBlank()) {
            p.setPromoLabel(null);
        }

        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                p.setImageUrl(imageUploadService.store(imageFile));
            } else if (imageUrl != null && !imageUrl.isBlank()) {
                p.setImageUrl(imageUrl.trim());
            }
        } catch (IllegalArgumentException | IOException e) {
            ra.addFlashAttribute("flashError", "Échec upload image : " + e.getMessage());
            return "redirect:/vendor/products/" + id + "/edit";
        }

        productRepository.save(p);
        ra.addFlashAttribute("flashOk", "Produit mis à jour !");
        return "redirect:/vendor/products";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id,
                                HttpSession session,
                                RedirectAttributes ra) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;

        VendorUser vendor = currentVendor(session);
        Product p = productRepository.findById(id).orElse(null);
        if (p == null || p.getVendor() == null || !p.getVendor().getId().equals(vendor.getId())) {
            ra.addFlashAttribute("flashError", "Produit introuvable ou accès refusé.");
            return "redirect:/vendor/products";
        }

        productRepository.deleteById(id);
        ra.addFlashAttribute("flashOk", "Produit supprimé.");
        return "redirect:/vendor/products";
    }

    // ─── Messages vendeur ───────────────────────────────────────────────────────────

    @GetMapping("/messages")
    @Transactional(readOnly = true)
    public String messagesInbox(HttpSession session, Model model) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;

        VendorUser vendor = currentVendor(session);

        // Chat réservé aux PRO/PREMIUM
        if (!vendor.canChat()) {
            model.addAttribute("pageTitle", "Messages — BOLA Vendeur");
            model.addAttribute("vendor", vendor);
            model.addAttribute("flashError",
                    "Le chat avec vos clients est disponible à partir du Pack Pro. Passez au niveau supérieur !");
            model.addAttribute("conversations", List.of());
            model.addAttribute("unreadCount", 0L);
            return "vendor/messages";
        }

        List<String> customerIds = chatMessageRepository.findDistinctCustomersByVendor(vendor);
        List<ChatMessage> conversations = customerIds.stream()
                .map(cid -> chatMessageRepository.findFirstByVendorAndCustomerIdentifierOrderBySentAtDesc(vendor, cid))
                .filter(m -> m != null)
                .sorted((a, b) -> b.getSentAt().compareTo(a.getSentAt()))
                .toList();

        long unread = chatMessageRepository.countByVendorAndReadByVendorFalseAndSenderType(vendor, "CUSTOMER");

        model.addAttribute("pageTitle",    "Messages — BOLA Vendeur");
        model.addAttribute("vendor",       vendor);
        model.addAttribute("conversations", conversations);
        model.addAttribute("unreadCount",  unread);
        return "vendor/messages";
    }

    @GetMapping("/messages/{custId}")
    @Transactional
    public String messageThread(@PathVariable String custId,
                                HttpSession session,
                                Model model) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;

        VendorUser vendor = currentVendor(session);
        List<ChatMessage> messages = chatMessageRepository
                .findByVendorAndCustomerIdentifierOrderBySentAtAsc(vendor, custId);

        // Marquer comme lus par le vendeur
        messages.stream()
                .filter(m -> "CUSTOMER".equals(m.getSenderType()) && !m.isReadByVendor())
                .forEach(m -> { m.setReadByVendor(true); chatMessageRepository.save(m); });

        String customerName = messages.isEmpty() ? custId : 
                (messages.get(0).getCustomerName() != null ? messages.get(0).getCustomerName() : custId);

        model.addAttribute("pageTitle",     "Chat avec " + customerName + " — BOLA");
        model.addAttribute("vendor",        vendor);
        model.addAttribute("messages",      messages);
        model.addAttribute("customerName",  customerName);
        model.addAttribute("custId",        custId);
        return "vendor/message-thread";
    }

    @PostMapping("/messages/{custId}/send")
    public String sendMessageToCustomer(@PathVariable String custId,
                                        @RequestParam String message,
                                        HttpSession session) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;

        VendorUser vendor = currentVendor(session);
        if (message != null && !message.isBlank()) {
            ChatMessage msg = new ChatMessage();
            msg.setVendor(vendor);
            msg.setCustomerIdentifier(custId);
            msg.setSenderType("VENDOR");
            msg.setMessage(message.trim());
            msg.setSentAt(Instant.now());
            msg.setReadByVendor(true);
            msg.setReadByCustomer(false);
            chatMessageRepository.save(msg);
        }
        return "redirect:/vendor/messages/" + custId;
    }

    // ─── Proposer un livreur ──────────────────────────────────────────────────

    @GetMapping("/couriers")
    @Transactional(readOnly = true)
    public String courierApplicationsPage(HttpSession session, Model model) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;

        VendorUser vendor = currentVendor(session);
        model.addAttribute("pageTitle", "Mes livreurs proposés — BOLA");
        model.addAttribute("vendor", vendor);
        model.addAttribute("applications",
                courierApplicationRepository.findByVendorOrderBySubmittedAtDesc(vendor));
        return "vendor/couriers";
    }

    @PostMapping("/couriers/propose")
    public String proposeCourier(@RequestParam String courierName,
                                 @RequestParam String courierPhone,
                                 @RequestParam(required = false) String zone,
                                 @RequestParam(value = "courierPhoto", required = false) MultipartFile courierPhoto,
                                 @RequestParam(value = "courierIdDoc", required = false) MultipartFile courierIdDoc,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;

        VendorUser vendor = currentVendor(session);

        String cleanName  = sanitizer.sanitizeText(courierName);
        String cleanPhone = sanitizer.sanitizeText(courierPhone);
        String cleanZone  = sanitizer.sanitizeText(zone);

        if (cleanName == null || cleanName.isBlank()) {
            ra.addFlashAttribute("flashError", "Le nom du livreur est obligatoire.");
            return "redirect:/vendor/couriers";
        }
        if (cleanPhone == null || cleanPhone.isBlank()) {
            ra.addFlashAttribute("flashError", "Le téléphone du livreur est obligatoire.");
            return "redirect:/vendor/couriers";
        }
        if (courierPhoto == null || courierPhoto.isEmpty()) {
            ra.addFlashAttribute("flashError", "La photo du livreur est obligatoire.");
            return "redirect:/vendor/couriers";
        }
        if (courierIdDoc == null || courierIdDoc.isEmpty()) {
            ra.addFlashAttribute("flashError", "La pièce d'identité du livreur est obligatoire.");
            return "redirect:/vendor/couriers";
        }

        // Upload photo livreur
        String photoUrl;
        try {
            photoUrl = imageUploadService.store(courierPhoto);
        } catch (IllegalArgumentException | IOException e) {
            ra.addFlashAttribute("flashError", "Photo invalide : " + e.getMessage());
            return "redirect:/vendor/couriers";
        }

        // Upload CNI livreur + scan Vision
        String idDocUrl;
        try {
            idDocUrl = imageUploadService.store(courierIdDoc);
        } catch (IllegalArgumentException | IOException e) {
            ra.addFlashAttribute("flashError", "Pièce d'identité invalide : " + e.getMessage());
            return "redirect:/vendor/couriers";
        }

        Boolean idVerified = idVerificationService.verify(courierIdDoc);
        if (Boolean.FALSE.equals(idVerified)) {
            ra.addFlashAttribute("flashError",
                    "La pièce d'identité du livreur ne semble pas être un document officiel. " +
                    "Veuillez uploader une photo nette de sa CNI, passeport ou permis.");
            return "redirect:/vendor/couriers";
        }

        CourierApplication app = new CourierApplication();
        app.setCourierName(cleanName);
        app.setCourierPhone(cleanPhone);
        app.setZone(cleanZone);
        app.setPhotoUrl(photoUrl);
        app.setIdDocumentUrl(idDocUrl);
        app.setIdDocVerified(idVerified);
        app.setVendor(vendor);
        courierApplicationRepository.save(app);
        log.info("📱 Proposition livreur sauvegardée: {}", cleanName);

        // Envoi automatique via Meta Cloud API (si configuré)
        log.info("   → Envoi notification WhatsApp admin...");
        try {
            String adminWhatsApp = whatsAppService.getAdminWhatsApp();
            if (adminWhatsApp == null || adminWhatsApp.isBlank()) {
                log.warn("⚠️ Admin WhatsApp non configuré - notification non envoyée");
            } else {
                String notifMsg = "🚴 Nouvelle proposition de livreur sur BOLA !\n\n"
                        + "🏪 Vendeur : " + vendor.getDisplayName() + "\n"
                        + "👤 Livreur : " + cleanName + "\n"
                        + "📞 Téléphone : " + cleanPhone + "\n"
                        + (cleanZone != null ? "📍 Zone : " + cleanZone + "\n" : "")
                        + "\n→ Validez depuis l'admin BOLA";
                metaWhatsApp.sendText(adminWhatsApp, notifMsg);
                log.info("✅ Notification WhatsApp livreur envoyée à {} pour {}", adminWhatsApp, cleanName);
            }
        } catch (Exception e) {
            log.error("❌ Erreur notification WhatsApp livreur (proposition sauvegardée quand même): {}", e.getMessage(), e);
        }

        ra.addFlashAttribute("flashOk", "Demande envoyée ! L'admin validera ce livreur sous 24h.");
        return "redirect:/vendor/couriers";
    }

    // ─── Demande de nouvelle catégorie ───────────────────────────────────────

    @PostMapping("/products/request-category")
    @ResponseBody
    public ResponseEntity<String> requestCategory(@RequestParam String categoryName,
                                                  HttpSession session) {
        if (currentVendor(session) == null) {
            return ResponseEntity.status(401).body("Non connecté");
        }

        VendorUser vendor = currentVendor(session);
        String cleanName = sanitizer.sanitizeText(categoryName);
        if (cleanName == null || cleanName.isBlank()) {
            return ResponseEntity.badRequest().body("Nom de catégorie invalide.");
        }

        // Notifier l'admin via WhatsApp
        try {
            String adminPhone = whatsAppService.getAdminWhatsApp();
            if (adminPhone != null && !adminPhone.isBlank()) {
                String msg = "📂 Demande de nouvelle catégorie sur BOLA !\n\n"
                        + "🏪 Vendeur : " + vendor.getDisplayName() + "\n"
                        + "📞 Téléphone : " + vendor.getPhone() + "\n"
                        + "💡 Catégorie demandée : " + cleanName + "\n\n"
                        + "→ Créez-la depuis l'admin BOLA si approprié.";
                metaWhatsApp.sendText(adminPhone, msg);
            }
        } catch (Exception e) {
            log.warn("⚠️ Notif WhatsApp demande catégorie échouée : {}", e.getMessage());
        }

        return ResponseEntity.ok("Demande envoyée !");
    }

    // ─── Cartes de fidélité ─────────────────────────────────────────

    @GetMapping("/loyalty")
    @Transactional(readOnly = true)
    public String loyaltyPage(HttpSession session, Model model) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;
        VendorUser vendor = currentVendor(session);
        model.addAttribute("pageTitle", "Cartes fidélité — BOLA Vendeur");
        model.addAttribute("vendor", vendor);
        model.addAttribute("cards", loyaltyCardRepository.findByVendorOrderByActiveDescCreatedAtDesc(vendor));
        return "vendor/loyalty";
    }

    @PostMapping("/loyalty/create")
    @Transactional
    public String createLoyaltyCard(@RequestParam String customerName,
                                    @RequestParam String customerPhone,
                                    @RequestParam(defaultValue = "10") int discountPercent,
                                    @RequestParam(required = false) String expiresAt,
                                    @RequestParam(required = false) String notes,
                                    HttpSession session,
                                    RedirectAttributes ra) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;
        VendorUser vendor = currentVendor(session);

        String cleanName  = sanitizer.sanitizeText(customerName);
        String cleanPhone = sanitizer.sanitizeText(customerPhone);
        String cleanNotes = sanitizer.sanitizeText(notes);

        if (cleanName == null || cleanName.isBlank()) {
            ra.addFlashAttribute("flashError", "Le nom du client est obligatoire.");
            return "redirect:/vendor/loyalty";
        }
        if (cleanPhone == null || cleanPhone.isBlank()) {
            ra.addFlashAttribute("flashError", "Le téléphone du client est obligatoire.");
            return "redirect:/vendor/loyalty";
        }
        if (discountPercent < 1 || discountPercent > 100) {
            ra.addFlashAttribute("flashError", "La réduction doit être entre 1 et 100%.");
            return "redirect:/vendor/loyalty";
        }

        LoyaltyCard card = new LoyaltyCard();
        card.setVendor(vendor);
        card.setCustomerName(cleanName);
        card.setCustomerPhone(cleanPhone);
        card.setDiscountPercent(discountPercent);
        if (cleanNotes != null && !cleanNotes.isBlank()) card.setNotes(cleanNotes);
        if (expiresAt != null && !expiresAt.isBlank()) {
            try { card.setExpiresAt(java.time.LocalDate.parse(expiresAt)); }
            catch (Exception ignored) {}
        }
        // Générer un code unique : 3 lettres boutique + 6 chiffres aléatoires
        String prefix = vendor.getDisplayName().replaceAll("[^A-Za-z]", "").toUpperCase();
        prefix = prefix.length() >= 3 ? prefix.substring(0, 3) : "BOL";
        String code = prefix + "-" + String.format("%06d", (int)(Math.random() * 1_000_000));
        card.setCode(code);

        loyaltyCardRepository.save(card);
        ra.addFlashAttribute("flashOk",
                "Carte créée pour " + cleanName + " ! Code : " + code);
        return "redirect:/vendor/loyalty";
    }

    @PostMapping("/loyalty/{id}/toggle")
    @Transactional
    public String toggleLoyaltyCard(@PathVariable Long id,
                                    HttpSession session,
                                    RedirectAttributes ra) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;
        VendorUser vendor = currentVendor(session);
        loyaltyCardRepository.findById(id).ifPresent(card -> {
            if (card.getVendor().getId().equals(vendor.getId())) {
                card.setActive(!card.isActive());
                loyaltyCardRepository.save(card);
            }
        });
        ra.addFlashAttribute("flashOk", "Statut de la carte mis à jour.");
        return "redirect:/vendor/loyalty";
    }

    @PostMapping("/loyalty/{id}/delete")
    @Transactional
    public String deleteLoyaltyCard(@PathVariable Long id,
                                    HttpSession session,
                                    RedirectAttributes ra) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;
        VendorUser vendor = currentVendor(session);
        loyaltyCardRepository.findById(id).ifPresent(card -> {
            if (card.getVendor().getId().equals(vendor.getId())) {
                loyaltyCardRepository.deleteById(id);
            }
        });
        ra.addFlashAttribute("flashOk", "Carte supprimée.");
        return "redirect:/vendor/loyalty";
    }

    // ─── Statistiques vendeur ────────────────────────────────────────────────

    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public String statsPage(HttpSession session, Model model) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;

        VendorUser vendor = currentVendor(session);

        if (!vendor.canViewStats()) {
            model.addAttribute("pageTitle", "Statistiques — BOLA Vendeur");
            model.addAttribute("vendor", vendor);
            model.addAttribute("flashError",
                    "Les statistiques sont disponibles à partir du Pack Pro. Passez au niveau supérieur !");
            return "vendor/stats";
        }

        try {
            if (vendor.hasAdvancedStats()) {
                model.addAttribute("stats", vendorStatsService.getAdvancedStats(vendor));
            } else {
                model.addAttribute("stats", vendorStatsService.getBasicStats(vendor));
            }
        } catch (Exception e) {
            log.warn("Erreur chargement stats : {}", e.getMessage());
        }

        model.addAttribute("pageTitle", "Statistiques — BOLA Vendeur");
        model.addAttribute("vendor", vendor);
        return "vendor/stats";
    }

    // ─── Avis clients ──────────────────────────────────────────────────────

    @GetMapping("/reviews")
    @Transactional(readOnly = true)
    public String reviewsPage(HttpSession session, Model model) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;

        VendorUser vendor = currentVendor(session);
        var reviews = reviewRepository.findApprovedByVendor(vendor);
        double avgRating = reviewRepository.averageRatingByVendor(vendor);

        model.addAttribute("pageTitle", "Avis clients — BOLA Vendeur");
        model.addAttribute("vendor",    vendor);
        model.addAttribute("reviews",   reviews);
        model.addAttribute("avgRating", avgRating);
        return "vendor/reviews";
    }

    @PostMapping("/reviews/{id}/reply")
    @Transactional
    public String replyToReview(@PathVariable Long id,
                                @RequestParam String reply,
                                HttpSession session,
                                RedirectAttributes ra) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;

        VendorUser vendor = currentVendor(session);
        if (!vendor.canRespondToReviews()) {
            ra.addFlashAttribute("flashError", "La réponse aux avis est réservée au Pack Premium.");
            return "redirect:/vendor/reviews";
        }

        reviewRepository.findById(id).ifPresent(review -> {
            // Vérifier que l'avis concerne un produit de ce vendeur
            if (review.getProduct() != null
                    && review.getProduct().getVendor() != null
                    && review.getProduct().getVendor().getId().equals(vendor.getId())) {
                String cleanReply = sanitizer.sanitizeText(reply);
                if (cleanReply != null && !cleanReply.isBlank()) {
                    review.setVendorReply(cleanReply);
                    review.setVendorReplyAt(Instant.now());
                    reviewRepository.save(review);
                }
            }
        });
        ra.addFlashAttribute("flashOk", "Réponse publiée !");
        return "redirect:/vendor/reviews";
    }

    // ─── API Polling pour le chat (refresh automatique 5s) ──────────────────

    @GetMapping("/api/messages/poll")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> pollMessages(HttpSession session) {
        VendorUser vendor = currentVendor(session);
        if (vendor == null || !vendor.canChat()) {
            return ResponseEntity.status(401).body(Map.of("error", "Non autorisé"));
        }

        long unread = chatMessageRepository.countByVendorAndReadByVendorFalseAndSenderType(vendor, "CUSTOMER");
        Map<String, Object> result = new HashMap<>();
        result.put("unreadCount", unread);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/messages/{custId}/poll")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> pollThread(@PathVariable String custId,
                                                                 HttpSession session) {
        VendorUser vendor = currentVendor(session);
        if (vendor == null || !vendor.canChat()) {
            return ResponseEntity.status(401).body(List.of());
        }

        var messages = chatMessageRepository
                .findByVendorAndCustomerIdentifierOrderBySentAtAsc(vendor, custId);
        var result = messages.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("senderType", m.getSenderType());
            map.put("message", m.getMessage());
            map.put("sentAt", m.getSentAt().toString());
            map.put("customerName", m.getCustomerName());
            return map;
        }).toList();
        return ResponseEntity.ok(result);
    }
}
