package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.dto.CourierUpdateDto;
import com.bolas.ecommerce.dto.NewOrderDto;
import com.bolas.ecommerce.model.Category;
import com.bolas.ecommerce.model.CustomerOrder;
import com.bolas.ecommerce.model.DeliveryOption;
import com.bolas.ecommerce.model.OrderLine;
import com.bolas.ecommerce.model.OrderStatus;
import com.bolas.ecommerce.model.Product;
import com.bolas.ecommerce.model.VendorPlan;
import com.bolas.ecommerce.model.VendorUser;
import com.bolas.ecommerce.model.VendorStatus;
import com.bolas.ecommerce.model.CourierApplicationStatus;
import com.bolas.ecommerce.model.Country;
import com.bolas.ecommerce.model.PriceChangeHistory;
import com.bolas.ecommerce.repository.CategoryRepository;
import com.bolas.ecommerce.repository.ChatMessageRepository;
import com.bolas.ecommerce.repository.CustomerOrderRepository;
import com.bolas.ecommerce.repository.LoyaltyCardRepository;
import com.bolas.ecommerce.repository.OrderLineRepository;
import com.bolas.ecommerce.repository.ProductRepository;
import com.bolas.ecommerce.repository.VendorUserRepository;
import com.bolas.ecommerce.repository.CourierApplicationRepository;
import com.bolas.ecommerce.repository.CountryRepository;
import com.bolas.ecommerce.repository.VendorCategoryRepository;
import com.bolas.ecommerce.repository.ReportRepository;
import com.bolas.ecommerce.repository.PriceChangeHistoryRepository;
import com.bolas.ecommerce.service.SessionCounterService;
import com.bolas.ecommerce.service.WhatsAppNotificationService;
import com.bolas.ecommerce.service.AuditLogService;
import com.bolas.ecommerce.service.CategoryCoverImageUrlService;
import com.bolas.ecommerce.service.CategoryCoverImageUrlService.Resolution;
import com.bolas.ecommerce.service.CategoryCoverImageUrlService.ResolutionKind;
import com.bolas.ecommerce.service.ImageUploadService;
import com.bolas.ecommerce.service.InputSanitizerService;
import com.bolas.ecommerce.service.OrderFlowService;
import com.bolas.ecommerce.service.PackPricingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final LoyaltyCardRepository loyaltyCardRepository;
    private final OrderLineRepository orderLineRepository;
    private final ImageUploadService imageUploadService;
    private final CategoryCoverImageUrlService categoryCoverImageUrlService;
    private final InputSanitizerService inputSanitizerService;
    private final AuditLogService auditLogService;
    private final OrderFlowService orderFlowService;
    private final VendorUserRepository vendorUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final CourierApplicationRepository courierApplicationRepository;
    private final WhatsAppNotificationService whatsAppNotificationService;
    private final CountryRepository countryRepository;
    private final VendorCategoryRepository vendorCategoryRepository;
    private final ReportRepository reportRepository;
    private final SessionCounterService sessionCounter;
    private final PackPricingService packPricingService;
    private final com.bolas.ecommerce.repository.ShopSellerRepository shopSellerRepository;
    private final PriceChangeHistoryRepository priceChangeHistoryRepository;
    private final com.bolas.ecommerce.service.VendorTrustScoreService trustScoreService;

    @Value("${google.maps.api.key:}")
    private String googleMapsApiKey;

    @Value("${bolas.shop.latitude:5.3600}")
    private double shopLatitude;

    @Value("${bolas.shop.longitude:-3.9903}")
    private double shopLongitude;

    public AdminController(ProductRepository productRepository,
                           CategoryRepository categoryRepository,
                           ChatMessageRepository chatMessageRepository,
                           CustomerOrderRepository customerOrderRepository,
                           LoyaltyCardRepository loyaltyCardRepository,
                           OrderLineRepository orderLineRepository,
                           ImageUploadService imageUploadService,
                           CategoryCoverImageUrlService categoryCoverImageUrlService,
                           InputSanitizerService inputSanitizerService,
                           AuditLogService auditLogService,
                           OrderFlowService orderFlowService,
                           VendorUserRepository vendorUserRepository,
                           PasswordEncoder passwordEncoder,
                           CourierApplicationRepository courierApplicationRepository,
                           WhatsAppNotificationService whatsAppNotificationService,
                           CountryRepository countryRepository,
                           VendorCategoryRepository vendorCategoryRepository,
                           ReportRepository reportRepository,
                           SessionCounterService sessionCounter,
                           PackPricingService packPricingService,
                           com.bolas.ecommerce.repository.ShopSellerRepository shopSellerRepository,
                           PriceChangeHistoryRepository priceChangeHistoryRepository,
                           com.bolas.ecommerce.service.VendorTrustScoreService trustScoreService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.loyaltyCardRepository = loyaltyCardRepository;
        this.orderLineRepository = orderLineRepository;
        this.imageUploadService = imageUploadService;
        this.categoryCoverImageUrlService = categoryCoverImageUrlService;
        this.inputSanitizerService = inputSanitizerService;
        this.auditLogService = auditLogService;
        this.orderFlowService = orderFlowService;
        this.vendorUserRepository = vendorUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.courierApplicationRepository = courierApplicationRepository;
        this.whatsAppNotificationService = whatsAppNotificationService;
        this.countryRepository = countryRepository;
        this.vendorCategoryRepository = vendorCategoryRepository;
        this.reportRepository = reportRepository;
        this.sessionCounter = sessionCounter;
        this.packPricingService = packPricingService;
        this.shopSellerRepository = shopSellerRepository;
        this.priceChangeHistoryRepository = priceChangeHistoryRepository;
        this.trustScoreService = trustScoreService;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Long.class, "promoPriceCfa", new CustomNumberEditor(Long.class, true));
    }

    @GetMapping("/admin")
    public String adminRoot() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/dashboard")
    @Transactional(readOnly = true)
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Tableau de bord — Bola's");
        model.addAttribute("productCount", productRepository.count());
        model.addAttribute("categoryCount", categoryRepository.count());
        model.addAttribute("orderCount", customerOrderRepository.count());
        model.addAttribute("recentOrders", customerOrderRepository.findTop10ByOrderByCreatedAtDesc());
        model.addAttribute("pendingVendorCount",
                vendorUserRepository.countByVendorStatus(VendorStatus.PENDING));
        model.addAttribute("pendingSubscriptionCount",
                vendorUserRepository.countPendingSubscriptions());

        // Vendeurs actifs
        var activeVendors = vendorUserRepository.findByVendorStatusAndActiveTrue(VendorStatus.ACTIVE);
        model.addAttribute("activeVendorCount", activeVendors.size());

        // Nouveaux vendeurs cette semaine
        java.time.LocalDateTime weekAgo = java.time.LocalDateTime.now().minusDays(7);
        // (approximation via ID — les nouveaux ont les IDs les plus élevés)
        model.addAttribute("newVendorsWeek", 0); // enrichi si createdAt disponible

        // Abonnements expirant dans <= 5 jours
        java.time.LocalDateTime today = java.time.LocalDateTime.now();
        java.time.LocalDateTime in5days = today.plusDays(5);
        var expiring = vendorUserRepository.findBySubscriptionExpiresAtBetween(today, in5days);
        model.addAttribute("expiringVendors", expiring);

        List<Category> categories = categoryRepository.findAll();
        List<String> chartLabels = new ArrayList<>();
        List<Long> chartCounts = new ArrayList<>();
        for (Category c : categories) {
            chartLabels.add(c.getName());
            chartCounts.add(productRepository.countByCategory(c));
        }
        model.addAttribute("categoryChartLabels", chartLabels);
        model.addAttribute("categoryChartCounts", chartCounts);
        return "admin/dashboard";
    }

    // ─── admin/products SUPPRIMÉ — gestion produits côté vendeur uniquement ─────
    // Conformément au PRINCIPE FONDAMENTAL BOLA : chaque boutique est indépendante.
    // Redirige vers le dashboard pour éviter les 404 si lien obsolète.
    @GetMapping("/admin/products")
    public String productsRedirect() { return "redirect:/admin/dashboard"; }

    @GetMapping("/admin/products/new")
    public String newProductRedirect() { return "redirect:/admin/dashboard"; }

    @GetMapping("/admin/products/{id}/edit")
    public String editProductRedirect(@PathVariable Long id) { return "redirect:/admin/dashboard"; }

    @PostMapping("/admin/products")
    public String saveProductRedirect() { return "redirect:/admin/dashboard"; }

    @PostMapping("/admin/products/{id}/toggle-sponsored")
    public String toggleSponsoredRedirect(@PathVariable Long id) { return "redirect:/admin/dashboard"; }

    @PostMapping("/admin/products/{id}/delete")
    public String deleteProductRedirect(@PathVariable Long id) { return "redirect:/admin/dashboard"; }

    @GetMapping("/admin/categories")
    public String categories(@ModelAttribute("category") Category category, Model model) {
        model.addAttribute("pageTitle", "Catégories — Admin Bola's");
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/categories";
    }

    @GetMapping("/admin/categories/{id}/edit")
    public String editCategory(@PathVariable Long id, Model model) {
        Category c = categoryRepository.findById(id).orElseThrow();
        model.addAttribute("pageTitle", "Catégories — Admin Bola's");
        model.addAttribute("category", c);
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/categories";
    }

    @PostMapping("/admin/categories")
    public String saveCategory(@Valid @ModelAttribute Category category,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        category.setName(inputSanitizerService.sanitizeText(category.getName()));
        category.setDescription(inputSanitizerService.sanitizeText(category.getDescription()));
        category.setImageUrl(inputSanitizerService.sanitizeText(category.getImageUrl()));
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("flashError", "Vérifiez les champs de la catégorie.");
            return "redirect:/admin/categories";
        }

        Resolution img = categoryCoverImageUrlService.resolveCoverUrl(category.getImageUrl());
        String imageUrl = img.url().isEmpty() ? null : img.url();
        if (img.kind() == ResolutionKind.CONVERTED_FROM_PINTEREST) {
            redirectAttributes.addFlashAttribute("flashOk",
                    "Lien Pinterest converti en image directe (i.pinimg.com). Enregistrement effectué.");
        } else if (img.kind() == ResolutionKind.PINTEREST_UNRESOLVED) {
            redirectAttributes.addFlashAttribute("flashError",
                    "Impossible de recuperer l'image depuis ce lien Pinterest (souvent un blocage cote Pinterest). "
                            + "Ouvrez l'epingle dans le navigateur, clic droit sur l'image -> Copier l'adresse de l'image "
                            + "(URL en i.pinimg.com) et collez-la ici.");
        }

        if (category.getId() != null) {
            Category existing = categoryRepository.findById(category.getId()).orElseThrow();
            existing.setName(category.getName());
            existing.setDescription(category.getDescription());
            existing.setImageUrl(imageUrl);
            categoryRepository.save(existing);
            auditLogService.categoryUpdated(existing.getId(), existing.getName());
        } else {
            category.setImageUrl(imageUrl);
            Category saved = categoryRepository.save(category);
            auditLogService.categoryCreated(saved.getId(), saved.getName());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/admin/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Category cat = categoryRepository.findById(id).orElseThrow();
        if (productRepository.countByCategory(cat) > 0) {
            redirectAttributes.addFlashAttribute("flashError",
                    "Impossible de supprimer : des produits sont encore dans cette catégorie.");
            return "redirect:/admin/categories";
        }
        String catName = cat.getName();
        categoryRepository.deleteById(id);
        auditLogService.categoryDeleted(id, catName);
        return "redirect:/admin/categories";
    }

    // ─── admin/orders SUPPRIMÉ — gestion commandes côté vendeur uniquement ──────
    // Conformément au PRINCIPE FONDAMENTAL BOLA.
    @GetMapping("/admin/orders")
    public String ordersRedirect() { return "redirect:/admin/dashboard"; }

    @PostMapping("/admin/orders/new")
    public String createOrderRedirect() { return "redirect:/admin/dashboard"; }

    @PostMapping("/admin/orders/{id}/status")
    public String updateOrderStatusRedirect(@PathVariable Long id) { return "redirect:/admin/dashboard"; }

    @PostMapping("/admin/orders/{id}/confirm")
    public String confirmOrderRedirect(@PathVariable Long id) { return "redirect:/admin/dashboard"; }

    @PostMapping("/admin/orders/{id}/notify-client")
    public String notifyClientRedirect(@PathVariable Long id) { return "redirect:/admin/dashboard"; }

    // ─── Gestion des vendeurs ────────────────────────────────────────────────────

    @GetMapping("/admin/vendors")
    @Transactional(readOnly = true)
    public String vendors(Model model) {
        try {
            log.info("👥 Début chargement page vendeurs...");
            model.addAttribute("pageTitle", "Vendeurs — Admin BOLA");
            
            log.info("   → Recherche tous les vendeurs...");
            // Pageable : max 200 vendeurs par page pour éviter les timeouts
            var vendorPage = vendorUserRepository.findAll(
                    PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "id")));
            List<VendorUser> allVendors = vendorPage.getContent();
            model.addAttribute("vendors", allVendors);
            model.addAttribute("vendorTotalCount", vendorPage.getTotalElements());
            log.info("      ✓ {} vendeurs total trouvés", allVendors.size());
            
            log.info("   → Recherche vendeurs PENDING...");
            List<VendorUser> pending = vendorUserRepository.findByVendorStatus(VendorStatus.PENDING);
            model.addAttribute("pendingVendors", pending);
            log.info("      ✓ {} vendeurs PENDING trouvés", pending.size());
            
            log.info("   → Recherche vendeurs ACTIVE...");
            List<VendorUser> active = allVendors.stream()
                    .filter(v -> v.getVendorStatus() == VendorStatus.ACTIVE)
                    .toList();
            model.addAttribute("activeVendors", active);
            log.info("      ✓ {} vendeurs ACTIVE trouvés", active.size());

            // Vendeurs suspendus
            List<VendorUser> suspended = allVendors.stream()
                    .filter(v -> v.getVendorStatus() == VendorStatus.SUSPENDED)
                    .toList();
            model.addAttribute("suspendedVendors", suspended);

            // Catégories pour les modals d'assignation
            model.addAttribute("allCategories", categoryRepository.findAll());

            // Map vendorId → Set<categoryId> pour pré-cocher les cases
            java.util.Map<Long, java.util.Set<Long>> vendorCategoryMap = new java.util.HashMap<>();
            for (VendorUser v : active) {
                java.util.Set<Long> catIds = vendorCategoryRepository.findCategoriesByVendor(v)
                        .stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toSet());
                vendorCategoryMap.put(v.getId(), catIds);
            }
            model.addAttribute("vendorCategoryMap", vendorCategoryMap);
            
            log.info("   → Recherche demandes livreurs PENDING...");
            var pendingCouriers = courierApplicationRepository.findByStatusOrderBySubmittedAtDesc(CourierApplicationStatus.PENDING);
            model.addAttribute("pendingCouriers", pendingCouriers);
            log.info("      ✓ {} demandes livreurs PENDING trouvées", pendingCouriers.size());
            
            log.info("   → Recherche livreurs approuvés...");
            var allCouriers = courierApplicationRepository.findByStatusOrderBySubmittedAtDesc(CourierApplicationStatus.APPROVED);
            model.addAttribute("allCouriers", allCouriers);
            log.info("      ✓ {} livreurs approuvés trouvés", allCouriers.size());
            
            // Prix des packs pour le formulaire de tarification
            model.addAttribute("gratuitPrice", packPricingService.getGratuitPrice());
            model.addAttribute("proLocalPrice", packPricingService.getProLocalPrice());
            model.addAttribute("proPrice", packPricingService.getProPrice());
            model.addAttribute("premiumPrice", packPricingService.getPremiumPrice());

            log.info("✅ Page vendeurs chargée avec succès");
        } catch (Exception e) {
            log.error("❌ Erreur CRITIQUE chargement vendeurs", e);
            model.addAttribute("flashError", "Erreur serveur, veuillez réessayer.");
            model.addAttribute("vendors", List.of());
            model.addAttribute("pendingVendors", List.of());
            model.addAttribute("activeVendors", List.of());
            model.addAttribute("pendingCouriers", List.of());
            model.addAttribute("allCouriers", List.of());
        }
        return "admin/vendors";
    }

    @PostMapping("/admin/vendors")
    public String saveVendor(@RequestParam String username,
                             @RequestParam String password,
                             @RequestParam String phone,
                             @RequestParam(required = false) String shopName,
                             @RequestParam String email,
                             RedirectAttributes ra) {
        
        // Validations
        if (username == null || username.isBlank()) {
            ra.addFlashAttribute("flashError", "L'identifiant est obligatoire.");
            return "redirect:/admin/vendors";
        }
        if (email == null || email.isBlank()) {
            ra.addFlashAttribute("flashError", "L'email est obligatoire.");
            return "redirect:/admin/vendors";
        }
        if (vendorUserRepository.findByUsername(username).isPresent()) {
            ra.addFlashAttribute("flashError", "Ce nom d'utilisateur existe déjà.");
            return "redirect:/admin/vendors";
        }
        if (vendorUserRepository.findByEmail(email).isPresent()) {
            ra.addFlashAttribute("flashError", "Cet email est déjà utilisé par un autre vendeur.");
            return "redirect:/admin/vendors";
        }
        
        VendorUser v = new VendorUser();
        v.setUsername(username.trim());
        v.setPasswordHash(passwordEncoder.encode(password));
        v.setPhone(phone.trim());
        if (shopName != null && !shopName.isBlank()) v.setShopName(shopName.trim());
        v.setEmail(email.trim());
        v.setActive(true);  // Créé par admin = actif directement
        v.setVendorStatus(com.bolas.ecommerce.model.VendorStatus.ACTIVE);
        vendorUserRepository.save(v);
        
        log.info("✅ Vendeur créé par admin: {} ({})", v.getUsername(), v.getEmail());
        ra.addFlashAttribute("flashOk", "Vendeur créé et activé: " + v.getDisplayName());
        return "redirect:/admin/vendors";
    }

    @PostMapping("/admin/vendors/{id}/toggle")
    public String toggleVendor(@PathVariable Long id, RedirectAttributes ra) {
        VendorUser v = vendorUserRepository.findById(id).orElseThrow();
        if (v.isActive()) {
            // Désactiver → SUSPENDED
            v.setVendorStatus(VendorStatus.SUSPENDED);
            v.setActive(false);
        } else {
            // Réactiver → ACTIVE
            v.setVendorStatus(VendorStatus.ACTIVE);
            v.setActive(true);
            v.setSuspensionReason(null);
        }
        vendorUserRepository.save(v);
        ra.addFlashAttribute("flashOk", v.isActive() ? "Vendeur activé." : "Vendeur désactivé.");
        return "redirect:/admin/vendors";
    }

    @PostMapping("/admin/vendors/{id}/approve")
    public String approveVendor(@PathVariable Long id, RedirectAttributes ra) {
        VendorUser v = vendorUserRepository.findById(id).orElseThrow();
        v.setActive(true);
        v.setVendorStatus(VendorStatus.ACTIVE);
        v.setSuspensionReason(null);
        vendorUserRepository.save(v);
        ra.addFlashAttribute("flashOk",
                "Boutique \"" + v.getDisplayName() + "\" approuvée et activée !");
        // Rediriger vers la page d'origine si on vient des suspendus
        return "redirect:/admin/vendors";
    }

    @GetMapping("/admin/suspended-vendors")
    @Transactional(readOnly = true)
    public String suspendedVendors(Model model) {
        model.addAttribute("pageTitle", "Boutiques suspendues — Admin BOLA");
        List<VendorUser> suspended = vendorUserRepository.findByVendorStatus(VendorStatus.SUSPENDED);
        model.addAttribute("suspendedVendors", suspended);
        return "admin/suspended-vendors";
    }

    @PostMapping("/admin/vendors/{id}/renew-and-activate")
    public String renewAndActivate(@PathVariable Long id,
                                   @RequestParam String plan,
                                   @RequestParam(required = false) String startsAt,
                                   @RequestParam(required = false) String expiresAt,
                                   RedirectAttributes ra) {
        VendorUser v = vendorUserRepository.findById(id).orElseThrow();
        v.setActive(true);
        v.setVendorStatus(VendorStatus.ACTIVE);
        v.setSuspensionReason(null);
        try {
            v.setPlan(com.bolas.ecommerce.model.VendorPlan.valueOf(plan));
        } catch (Exception ignored) {}
        if (startsAt != null && !startsAt.isBlank()) {
            v.setSubscriptionStartsAt(java.time.LocalDateTime.parse(startsAt));
        }
        if (expiresAt != null && !expiresAt.isBlank()) {
            v.setSubscriptionExpiresAt(java.time.LocalDateTime.parse(expiresAt));
        }
        vendorUserRepository.save(v);
        ra.addFlashAttribute("flashOk",
                "Boutique \"" + v.getDisplayName() + "\" réactivée avec plan " + plan + ".");
        return "redirect:/admin/suspended-vendors";
    }

    @PostMapping("/admin/vendors/{id}/suspension-reason")
    public String updateSuspensionReason(@PathVariable Long id,
                                          @RequestParam(required = false, defaultValue = "") String reason,
                                          RedirectAttributes ra) {
        VendorUser v = vendorUserRepository.findById(id).orElseThrow();
        v.setSuspensionReason(reason.isBlank() ? null : reason.trim());
        vendorUserRepository.save(v);
        ra.addFlashAttribute("flashOk", "Raison de suspension mise à jour.");
        return "redirect:/admin/suspended-vendors";
    }

    @PostMapping("/admin/vendors/{id}/categories")
    @Transactional
    public String saveVendorCategories(@PathVariable Long id,
                                       @RequestParam(required = false) List<Long> categoryIds,
                                       RedirectAttributes ra) {
        VendorUser v = vendorUserRepository.findById(id).orElseThrow();
        vendorCategoryRepository.deleteByVendor(v);
        if (categoryIds != null) {
            for (Long catId : categoryIds) {
                categoryRepository.findById(catId).ifPresent(cat ->
                        vendorCategoryRepository.save(new com.bolas.ecommerce.model.VendorCategory(v, cat)));
            }
        }
        ra.addFlashAttribute("flashOk", "Catégories mises à jour pour " + v.getDisplayName());
        return "redirect:/admin/vendors";
    }

    @PostMapping("/admin/vendors/{id}/suspend")
    public String suspendVendor(@PathVariable Long id,
                                @RequestParam(required = false, defaultValue = "true") boolean soft,
                                @RequestParam(required = false, defaultValue = "") String reason,
                                RedirectAttributes ra) {
        VendorUser v = vendorUserRepository.findById(id).orElseThrow();
        v.setActive(false);
        v.setVendorStatus(VendorStatus.SUSPENDED);
        v.setSoftSuspend(soft);
        v.setSuspensionReason(reason.isBlank() ? null : reason.trim());

        // Suspension totale → masquer tous ses produits
        if (!soft) {
            productRepository.findByVendor(v).forEach(p -> {
                p.setAvailable(false);
                productRepository.save(p);
            });
        }
        vendorUserRepository.save(v);

        // Notifier le vendeur via WhatsApp si téléphone disponible
        String waLink = whatsAppNotificationService.buildVendorSuspensionLink(
                v.getPhone(), v.getDisplayName(), soft, reason.isBlank() ? null : reason.trim());
        ra.addFlashAttribute("flashOk", "Vendeur suspendu (" + (soft ? "douce" : "totale") + ").");
        ra.addFlashAttribute("waSuspendLink", waLink);
        return "redirect:/admin/vendors";
    }

    @GetMapping("/admin/vendors/{id}/manage")
    @Transactional(readOnly = true)
    public String manageVendor(@PathVariable Long id, Model model) {
        VendorUser v = vendorUserRepository.findById(id).orElseThrow();
        model.addAttribute("pageTitle", "Gérer " + v.getDisplayName() + " — Admin BOLA");
        model.addAttribute("vendor", v);
        model.addAttribute("allCategories", categoryRepository.findAll());
        model.addAttribute("assignedCategoryIds",
                vendorCategoryRepository.findCategoriesByVendor(v).stream()
                        .map(c -> c.getId()).toList());
        model.addAttribute("gratuitPrice", packPricingService.getGratuitPrice());
        model.addAttribute("proLocalPrice", packPricingService.getProLocalPrice());
        model.addAttribute("proPrice", packPricingService.getProPrice());
        model.addAttribute("premiumPrice", packPricingService.getPremiumPrice());
        return "admin/vendor-manage";
    }

    @PostMapping("/admin/vendors/{id}/plan")
    public String setVendorPlan(@PathVariable Long id,
                                @RequestParam(required = false) String plan,
                                @RequestParam(required = false) String startsAt,
                                @RequestParam(required = false) String expiresAt,
                                RedirectAttributes ra) {
        vendorUserRepository.findById(id).ifPresent(v -> {
            if (plan != null && !plan.isBlank()) {
                try { v.setPlan(com.bolas.ecommerce.model.VendorPlan.valueOf(plan)); }
                catch (IllegalArgumentException ignored) {}
            }
            v.setSubscriptionStartsAt(startsAt != null && !startsAt.isBlank()
                    ? java.time.LocalDateTime.parse(startsAt) : null);
            v.setSubscriptionExpiresAt(expiresAt != null && !expiresAt.isBlank()
                    ? java.time.LocalDateTime.parse(expiresAt) : null);
            vendorUserRepository.save(v);
        });
        ra.addFlashAttribute("flashOk", "Plan mis à jour.");
        return "redirect:/admin/vendors/" + id + "/manage";
    }

    @PostMapping("/admin/packs/prices")
    public String updatePackPrices(@RequestParam(required = false, defaultValue = "0") int gratuit,
                                   @RequestParam(required = false, defaultValue = "0") int proLocal,
                                   @RequestParam(required = false, defaultValue = "0") int premium,
                                   RedirectAttributes ra) {
        packPricingService.updatePrice("GRATUIT", gratuit);
        packPricingService.updatePrice("PRO_LOCAL", proLocal);
        packPricingService.updatePrice("PREMIUM", premium);
        ra.addFlashAttribute("flashOk", "Prix des packs mis à jour.");
        return "redirect:/admin/vendors";
    }

    // ─── /admin/plans — Gestion des prix des plans ────────────────────────────

    @GetMapping("/admin/plans")
    public String plansPage(Model model) {
        model.addAttribute("pageTitle", "Plans & Tarifs — Admin BOLA");
        model.addAttribute("gratuitPrice",   packPricingService.getGratuitPrice());
        model.addAttribute("proLocalPrice",  packPricingService.getProLocalPrice());
        model.addAttribute("proPrice",       packPricingService.getProPrice());
        model.addAttribute("premiumPrice",   packPricingService.getPremiumPrice());
        model.addAttribute("priceHistory",   priceChangeHistoryRepository.findTop20ByOrderByChangedAtDesc());
        return "admin/plans";
    }

    @PostMapping("/admin/plans/update")
    public String updatePlanPrice(@RequestParam String plan,
                                  @RequestParam int price,
                                  RedirectAttributes ra) {
        if ("GRATUIT".equals(plan)) {
            ra.addFlashAttribute("flashError", "Le plan Gratuit est toujours à 0 FCFA.");
            return "redirect:/admin/plans";
        }
        int oldPrice = packPricingService.getPriceForPlan(plan);
        packPricingService.updatePrice(plan, price);
        priceChangeHistoryRepository.save(new PriceChangeHistory(plan, oldPrice, price));
        ra.addFlashAttribute("flashOk", "Prix du plan " + plan + " mis à jour : " + price + " FCFA.");
        return "redirect:/admin/plans";
    }    @PostMapping("/admin/vendors/{id}/banner")
    public String setVendorBanner(@PathVariable Long id,
                                  @RequestParam(required = false) String bannerUrl,
                                  @RequestParam(value = "bannerFile", required = false) MultipartFile bannerFile,
                                  RedirectAttributes ra) {
        vendorUserRepository.findById(id).ifPresent(v -> {
            try {
                if (bannerFile != null && !bannerFile.isEmpty()) {
                    v.setBannerUrl(imageUploadService.store(bannerFile));
                } else {
                    v.setBannerUrl(bannerUrl != null && !bannerUrl.isBlank() ? bannerUrl.trim() : null);
                }
                vendorUserRepository.save(v);
            } catch (IOException e) {
                log.error("Erreur upload bannière", e);
            }
        });
        ra.addFlashAttribute("flashOk", "Bannière mise à jour.");
        return "redirect:/admin/vendors";
    }

    @PostMapping("/admin/vendors/{id}/delete")
    @Transactional
    public String deleteVendor(@PathVariable Long id, RedirectAttributes ra) {
        try {
            log.info("🗑️ Début suppression vendeur {}", id);
            VendorUser v = vendorUserRepository.findById(id).orElseThrow();
            String vendorName = v.getDisplayName();
            
            log.info("   → Suppression des messages de chat liés...");
            var chatMessages = chatMessageRepository.findByVendor(v);
            log.info("      {} messages de chat à supprimer", chatMessages.size());
            chatMessageRepository.deleteByVendor(v);
            
            log.info("   → Suppression des cartes de fidélité liées...");
            var loyaltyCards = loyaltyCardRepository.findByVendor(v);
            log.info("      {} cartes de fidélité à supprimer", loyaltyCards.size());
            loyaltyCardRepository.deleteByVendor(v);
            
            log.info("   → Suppression des produits du vendeur (OrderLines incluses)...");
            var products = productRepository.findByVendor(v);
            log.info("      {} produits à supprimer", products.size());
            products.forEach(p -> {
                // Supprimer les OrderLines liées à ce produit
                long olCount = orderLineRepository.countByProduct_Id(p.getId());
                if (olCount > 0) {
                    log.info("         → {} OrderLines supprimées pour produit {}", olCount, p.getId());
                    orderLineRepository.deleteByProduct_Id(p.getId());
                    orderLineRepository.flush();
                }
            });
            productRepository.deleteAll(products);
            productRepository.flush();
            log.info("   → Suppression des catégories liées...");
            // Supprimer les catégories liées
            var categories = vendorCategoryRepository.findByVendor(v);
            log.info("      {} catégories à supprimer", categories.size());
            vendorCategoryRepository.deleteByVendor(v);
            
            log.info("   → Suppression des demandes livreurs liées...");
            // Supprimer les demandes livreurs liées
            courierApplicationRepository.deleteByVendor(v);
            
            log.info("   → Suppression du vendeur...");
            vendorUserRepository.deleteById(id);
            vendorUserRepository.flush();
            
            ra.addFlashAttribute("flashOk", "Vendeur \"" + vendorName + "\" supprimé avec succès.");
            log.info("✅ Vendeur {} supprimé avec succès", vendorName);
        } catch (Exception e) {
            log.error("❌ Erreur suppression vendeur {}", id, e);
            ra.addFlashAttribute("flashError", "Erreur suppression : " + e.getMessage());
        }
        return "redirect:/admin/vendors";
    }

    // ─── Demandes de livreurs ─────────────────────────────────────────────────

    @PostMapping("/admin/couriers/{id}/approve")
    @Transactional
    public String approveCourier(@PathVariable Long id, RedirectAttributes ra) {
        try {
            courierApplicationRepository.findById(id).ifPresent(app -> {
                app.setStatus(CourierApplicationStatus.APPROVED);
                app.setLastActionAt(LocalDateTime.now());
                courierApplicationRepository.save(app);
                log.info("✅ Livreur {} approuvé", app.getCourierName());
            });
            ra.addFlashAttribute("flashOk", "Livreur approuvé.");
        } catch (Exception e) {
            log.error("❌ Erreur approbation livreur", e);
            ra.addFlashAttribute("flashError", "Erreur: " + e.getMessage());
        }
        return "redirect:/admin/vendors";
    }

    @PostMapping("/admin/couriers/{id}/reject")
    @Transactional
    public String rejectCourier(@PathVariable Long id, RedirectAttributes ra) {
        try {
            courierApplicationRepository.findById(id).ifPresent(app -> {
                app.setStatus(CourierApplicationStatus.REJECTED);
                app.setLastActionAt(LocalDateTime.now());
                courierApplicationRepository.save(app);
                log.info("✅ Livreur {} rejeté", app.getCourierName());
            });
            ra.addFlashAttribute("flashOk", "Demande rejetée.");
        } catch (Exception e) {
            log.error("❌ Erreur rejet livreur", e);
            ra.addFlashAttribute("flashError", "Erreur: " + e.getMessage());
        }
        return "redirect:/admin/vendors";
    }

    @PostMapping("/admin/couriers/{id}/suspend")
    @Transactional
    public String suspendCourier(@PathVariable Long id, 
                                 @RequestParam(defaultValue = "false") boolean total,
                                 @RequestParam(required = false) String reason,
                                 RedirectAttributes ra) {
        try {
            courierApplicationRepository.findById(id).ifPresent(app -> {
                CourierApplicationStatus newStatus = total ? 
                    CourierApplicationStatus.SUSPENDED_TOTAL : 
                    CourierApplicationStatus.SUSPENDED_SOFT;
                app.setStatus(newStatus);
                app.setSuspensionReason(reason != null ? reason.trim() : null);
                app.setLastActionAt(LocalDateTime.now());
                courierApplicationRepository.save(app);
                log.info("⚠️ Livreur {} suspendu ({})", app.getCourierName(), newStatus);
            });
            String suspType = total ? "totale" : "douce";
            ra.addFlashAttribute("flashOk", "Livreur suspendu (" + suspType + ").");
        } catch (Exception e) {
            log.error("❌ Erreur suspension livreur", e);
            ra.addFlashAttribute("flashError", "Erreur: " + e.getMessage());
        }
        return "redirect:/admin/vendors";
    }

    @PostMapping("/admin/couriers/{id}/delete")
    @Transactional
    public String deleteCourier(@PathVariable Long id, RedirectAttributes ra) {
        try {
            courierApplicationRepository.findById(id).ifPresent(app -> {
                String courierName = app.getCourierName();
                app.setStatus(CourierApplicationStatus.DELETED);
                app.setLastActionAt(LocalDateTime.now());
                courierApplicationRepository.delete(app);
                log.info("🗑️ Livreur {} supprimé", courierName);
                ra.addFlashAttribute("flashOk", "Livreur \"" + courierName + "\" supprimé.");
            });
        } catch (Exception e) {
            log.error("❌ Erreur suppression livreur", e);
            ra.addFlashAttribute("flashError", "Erreur suppression: " + e.getMessage());
        }
        return "redirect:/admin/vendors";
    }

    // ─── Activité des livreurs (monitoring admin) ──────────────────────────────

    @GetMapping("/admin/courier-activity")
    @Transactional(readOnly = true)
    public String courierActivity(Model model) {
        model.addAttribute("pageTitle", "Activité des livreurs — Admin BOLA");

        // JOIN FETCH vendor pour éviter LazyInitializationException
        var couriers = courierApplicationRepository.findAllWithVendorByStatusIn(
                List.of(CourierApplicationStatus.APPROVED,
                        CourierApplicationStatus.SUSPENDED_SOFT,
                        CourierApplicationStatus.SUSPENDED_TOTAL));

        // Construire les stats par livreur
        var courierStats = new java.util.ArrayList<java.util.Map<String, Object>>();
        for (var courier : couriers) {
            var stat = new java.util.HashMap<String, Object>();
            stat.put("courier", courier);
            stat.put("totalDeliveries", customerOrderRepository.countByAssignedCourierName(courier.getCourierName()));
            stat.put("deliveredCount", customerOrderRepository.countByAssignedCourierNameAndStatus(
                    courier.getCourierName(), OrderStatus.DELIVERED));
            stat.put("inDeliveryCount", customerOrderRepository.countByAssignedCourierNameAndStatus(
                    courier.getCourierName(), OrderStatus.IN_DELIVERY));
            stat.put("recentOrders", customerOrderRepository.findTop10ByAssignedCourierNameOrderByCreatedAtDesc(
                    courier.getCourierName()));
            // Commande EN COURS (pour la carte livreur)
            var activeOrders = customerOrderRepository.findByAssignedCourierNameAndStatus(
                    courier.getCourierName(), OrderStatus.IN_DELIVERY);
            stat.put("activeOrder", activeOrders.isEmpty() ? null : activeOrders.get(0));
            // Nom du vendeur propriétaire avec fallback
            String vendorLabel = (courier.getVendor() != null)
                    ? (courier.getVendor().getShopName() != null
                            ? courier.getVendor().getShopName()
                            : courier.getVendor().getUsername())
                    : "Livreur plateforme BOLA";
            stat.put("vendorLabel", vendorLabel);
            courierStats.add(stat);
        }

        model.addAttribute("courierStats", courierStats);
        model.addAttribute("totalCouriers", couriers.size());
        model.addAttribute("activeCouriers", couriers.stream()
                .filter(c -> c.getStatus() == CourierApplicationStatus.APPROVED).count());
        return "admin/courier-activity";
    }

    // ─── Activité des boutiques ───────────────────────────────────────────────

    @GetMapping("/admin/shop-activity")
    @Transactional(readOnly = true)
    public String shopActivity(Model model) {
        model.addAttribute("pageTitle", "Activité des boutiques — Admin BOLA");
        List<VendorUser> vendors = vendorUserRepository.findByVendorStatusAndActiveTrue(VendorStatus.ACTIVE);

        // Construire les stats par boutique
        List<java.util.Map<String, Object>> stats = new java.util.ArrayList<>();
        for (VendorUser v : vendors) {
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("vendor", v);
            long prodCount = productRepository.countByVendor(v);
            row.put("productCount", prodCount);
            // Compter les commandes contenant des produits de ce vendeur
            long orderCount = productRepository.findByVendor(v).stream()
                    .mapToLong(p -> orderLineRepository.countByProduct_Id(p.getId()))
                    .sum();
            row.put("orderLineCount", orderCount);
            // Sous-vendeurs
            row.put("sellerCount", shopSellerRepository.countByVendor(v));
            // Localisation
            row.put("hasLocation", v.hasLocation());
            stats.add(row);
        }
        // Trier par nb de commandes décroissant
        stats.sort((a, b) -> Long.compare((Long) b.get("orderLineCount"), (Long) a.get("orderLineCount")));
        model.addAttribute("shopStats", stats);
        return "admin/shop-activity";
    }

    // ─── Backstage : sous-vendeurs d'une boutique ────────────────────────────

    @GetMapping("/admin/vendors/{id}/sellers")
    @Transactional(readOnly = true)
    public String vendorSellersBackstage(@PathVariable Long id, Model model) {
        VendorUser vendor = vendorUserRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Vendeur introuvable"));
        model.addAttribute("pageTitle", "Vendeurs de " + vendor.getDisplayName() + " — Admin BOLA");
        model.addAttribute("vendor", vendor);
        model.addAttribute("sellers", shopSellerRepository.findByVendorOrderByCreatedAtDesc(vendor));
        return "admin/vendor-sellers";
    }

    // ─── Gestion des pays ─────────────────────────────────────────────────────

    @GetMapping("/admin/countries")
    @Transactional(readOnly = true)
    public String countries(Model model) {
        model.addAttribute("pageTitle", "Pays — Admin BOLA");
        model.addAttribute("countries", countryRepository.findAll());
        return "admin/countries";
    }

    @PostMapping("/admin/countries")
    public String saveCountry(@RequestParam String code,
                              @RequestParam String name,
                              @RequestParam(required = false, defaultValue = "") String flag,
                              @RequestParam(required = false, defaultValue = "0") int customsTaxPercent,
                              RedirectAttributes ra) {
        Country c = new Country();
        c.setCode(code.trim().toUpperCase());
        c.setName(inputSanitizerService.sanitizeText(name));
        c.setFlag(flag.trim());
        c.setCustomsTaxPercent(Math.max(0, Math.min(100, customsTaxPercent)));
        c.setActive(true);
        countryRepository.save(c);
        ra.addFlashAttribute("flashOk", "Pays ajouté : " + c.getName());
        return "redirect:/admin/countries";
    }

    @PostMapping("/admin/countries/{id}/toggle")
    public String toggleCountry(@PathVariable Long id, RedirectAttributes ra) {
        countryRepository.findById(id).ifPresent(c -> {
            c.setActive(!c.isActive());
            countryRepository.save(c);
        });
        return "redirect:/admin/countries";
    }

    @PostMapping("/admin/countries/{id}/delete")
    public String deleteCountry(@PathVariable Long id, RedirectAttributes ra) {
        countryRepository.deleteById(id);
        ra.addFlashAttribute("flashOk", "Pays supprimé.");
        return "redirect:/admin/countries";
    }

    private String baseUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() != 80 && request.getServerPort() != 443
                ? ":" + request.getServerPort() : "");
    }

    @PostMapping("/admin/orders/{id}/delete")
    public String deleteOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        CustomerOrder order = customerOrderRepository.findById(id).orElseThrow();
        String tracking = order.getTrackingNumber();
        customerOrderRepository.deleteById(id);
        auditLogService.orderDeleted(id, tracking);
        redirectAttributes.addFlashAttribute("flashOk", "Commande supprimée.");
        return "redirect:/admin/dashboard";
    }

    // --- ICI : LA MÉTHODE POUR LA PAGE DE LIVRAISON AVEC LA CLÉ API ---
    @GetMapping("/admin/delivery")
    @Transactional(readOnly = true)
    public String deliveryForm(Model model) {
        model.addAttribute("pageTitle", "Livraison GPS — Admin Bola's");
        model.addAttribute("shopLatitude", shopLatitude);
        model.addAttribute("shopLongitude", shopLongitude);
        model.addAttribute("courierUpdate", new CourierUpdateDto());
        model.addAttribute("orders", customerOrderRepository.findTop50ByOrderByCreatedAtDesc());
        return "admin/delivery-update";
    }

    @PostMapping("/admin/delivery/position")
    public String updateCourierPosition(@Valid @ModelAttribute("courierUpdate") CourierUpdateDto courierUpdate,
                                        BindingResult bindingResult,
                                        @RequestParam(value = "courierPhoto", required = false) MultipartFile courierPhoto,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Livraison GPS — Admin Bola's");
            model.addAttribute("shopLatitude", shopLatitude);
            model.addAttribute("shopLongitude", shopLongitude);
            model.addAttribute("orders", customerOrderRepository.findAllByOrderByCreatedAtDesc());
            return "admin/delivery-update";
        }
        CustomerOrder order = customerOrderRepository.findById(courierUpdate.getOrderId()).orElseThrow();
        order.setCourierLatitude(courierUpdate.getCourierLatitude());
        order.setCourierLongitude(courierUpdate.getCourierLongitude());

        if (courierUpdate.getCourierPhone() != null) {
            String p = inputSanitizerService.sanitizeText(courierUpdate.getCourierPhone());
            order.setCourierPhone((p == null || p.isEmpty()) ? null : p);
        }
        if (courierUpdate.getCourierVehiclePlate() != null) {
            String pl = inputSanitizerService.sanitizeText(courierUpdate.getCourierVehiclePlate());
            order.setCourierVehiclePlate((pl == null || pl.isEmpty()) ? null : pl);
        }

        if (courierPhoto != null && !courierPhoto.isEmpty()) {
            try {
                order.setCourierPhotoUrl(imageUploadService.store(courierPhoto));
            } catch (IllegalArgumentException e) {
                model.addAttribute("pageTitle", "Livraison GPS — Admin Bola's");
                model.addAttribute("shopLatitude", shopLatitude);
                model.addAttribute("shopLongitude", shopLongitude);
                model.addAttribute("orders", customerOrderRepository.findTop50ByOrderByCreatedAtDesc());
                model.addAttribute("flashError", "Photo livreur invalide ou format non supporté.");
                return "admin/delivery-update";
            } catch (IOException e) {
                model.addAttribute("pageTitle", "Livraison GPS — Admin Bola's");
                model.addAttribute("shopLatitude", shopLatitude);
                model.addAttribute("shopLongitude", shopLongitude);
                model.addAttribute("orders", customerOrderRepository.findTop50ByOrderByCreatedAtDesc());
                model.addAttribute("flashError", "Échec de l'enregistrement de la photo livreur.");
                return "admin/delivery-update";
            }
        }

        customerOrderRepository.save(order);
        auditLogService.courierPositionUpdated(
                order.getId(), order.getTrackingNumber(),
                courierUpdate.getCourierLatitude(), courierUpdate.getCourierLongitude());
        redirectAttributes.addFlashAttribute("flashOk",
                "Informations livreur enregistrées. Le client les verra sur la page de suivi (carte ~30 s).");
        return "redirect:/admin/delivery";
    }

    // ─── Analytics ───────────────────────────────────────────────────────────

    @GetMapping("/admin/analytics")
    @Transactional(readOnly = true)
    public String analytics(Model model) {
        model.addAttribute("pageTitle", "Analytics — Admin BOLA");
        model.addAttribute("activeVisitors", sessionCounter.getActiveVisitors());
        model.addAttribute("connectedVendors", sessionCounter.getConnectedVendors());

        // Commandes ce mois
        java.time.YearMonth thisMonth = java.time.YearMonth.now();
        java.time.Instant startOfMonth = thisMonth.atDay(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        java.time.Instant endOfMonth = thisMonth.atEndOfMonth().atTime(23,59,59).toInstant(java.time.ZoneOffset.UTC);
        long ordersThisMonth = customerOrderRepository.countByCreatedAtBetween(startOfMonth, endOfMonth);
        model.addAttribute("ordersThisMonth", ordersThisMonth);

        // Commandes par mois (12 derniers mois) — requête optimisée par mois
        java.util.List<String> monthLabels = new java.util.ArrayList<>();
        java.util.List<Long> monthCounts = new java.util.ArrayList<>();
        java.time.ZoneId zone = java.time.ZoneId.of("Africa/Abidjan"); // UTC+0 Togo/CI
        for (int i = 11; i >= 0; i--) {
            java.time.YearMonth ym = java.time.YearMonth.now(zone).minusMonths(i);
            java.time.Instant start = ym.atDay(1).atStartOfDay(zone).toInstant();
            java.time.Instant end = ym.atEndOfMonth().atTime(23, 59, 59).atZone(zone).toInstant();
            long count = customerOrderRepository.countByCreatedAtBetween(start, end);
            monthLabels.add(ym.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.FRENCH)
                    + " " + ym.getYear());
            monthCounts.add(count);
        }
        model.addAttribute("monthLabels", monthLabels);
        model.addAttribute("monthCounts", monthCounts);

        // Répartition par statut
        java.util.List<String> statusLabels = new java.util.ArrayList<>();
        java.util.List<Long> statusCounts = new java.util.ArrayList<>();
        for (com.bolas.ecommerce.model.OrderStatus s : com.bolas.ecommerce.model.OrderStatus.values()) {
            long c = customerOrderRepository.countByStatus(s);
            if (c > 0) { statusLabels.add(s.name()); statusCounts.add(c); }
        }
        model.addAttribute("statusLabels", statusLabels);
        model.addAttribute("statusCounts", statusCounts);

        // Répartition par plan vendeur
        java.util.List<String> planLabels = new java.util.ArrayList<>();
        java.util.List<Long> planCounts = new java.util.ArrayList<>();
        for (com.bolas.ecommerce.model.VendorPlan p : com.bolas.ecommerce.model.VendorPlan.values()) {
            long c = vendorUserRepository.countByPlan(p);
            if (c > 0) { planLabels.add(p.name()); planCounts.add(c); }
        }
        model.addAttribute("planLabels", planLabels);
        model.addAttribute("planCounts", planCounts);

        model.addAttribute("totalOrders", customerOrderRepository.count());
        model.addAttribute("activeVendorCount",
                vendorUserRepository.findByVendorStatusAndActiveTrue(VendorStatus.ACTIVE).size());
        model.addAttribute("customerCount", 0); // sera enrichi quand CustomerRepository sera injecté

        return "admin/analytics";
    }

    // ─── Signalements ────────────────────────────────────────────────────────

    @GetMapping("/admin/reports")
    @Transactional(readOnly = true)
    public String reports(Model model) {
        model.addAttribute("pageTitle", "Signalements — Admin BOLA");
        model.addAttribute("pendingReports", reportRepository.findByResolvedFalseOrderByCreatedAtAsc());
        model.addAttribute("resolvedReports", reportRepository.findByResolvedTrueOrderByCreatedAtDesc());
        return "admin/reports";
    }

    @PostMapping("/admin/reports/{id}/resolve")
    @Transactional
    public String resolveReport(@PathVariable Long id, RedirectAttributes ra) {
        reportRepository.findById(id).ifPresent(r -> {
            r.setResolved(true);
            reportRepository.save(r);
        });
        ra.addFlashAttribute("flashOk", "Signalement marqué comme traité.");
        return "redirect:/admin/reports";
    }

    @PostMapping("/admin/reports/{id}/delete")
    @Transactional
    public String deleteReport(@PathVariable Long id, RedirectAttributes ra) {
        reportRepository.deleteById(id);
        ra.addFlashAttribute("flashOk", "Signalement supprimé.");
        return "redirect:/admin/reports";
    }

    /** Génère un lien GPS unique pour le livreur et passe la commande en IN_DELIVERY. */
    @PostMapping("/admin/delivery/{id}/generate-token")
    public String generateCourierToken(@PathVariable Long id,
                                       @RequestParam(defaultValue = "") String baseUrl,
                                       RedirectAttributes ra) {
        CustomerOrder order = customerOrderRepository.findById(id).orElseThrow();
        String token = UUID.randomUUID().toString();
        order.setCourierToken(token);
        order.setStatus(OrderStatus.IN_DELIVERY);
        customerOrderRepository.save(order);
        auditLogService.orderStatusChanged(id, order.getTrackingNumber(), "IN_DELIVERY");

        String link = (baseUrl.isBlank() ? "" : baseUrl.stripTrailing()) + "/livreur/" + token;
        ra.addFlashAttribute("flashOk", "Lien livreur généré !");
        ra.addFlashAttribute("courierLink", link);
        return "redirect:/admin/delivery";
    }

    // admin/orders/{id}/dispatch supprimé — le dispatch est géré par le vendeur via /vendor/orders/{id}/dispatch

    @GetMapping("/admin/vendors/{id}/notify-plan")
public String notifyPlan(@PathVariable Long id) {
    VendorUser v = vendorUserRepository.findById(id).orElseThrow();
    String planName = v.getPlan() != null ? switch(v.getPlan().name()) {
        case "PREMIUM" -> "Premium";
        case "PRO_LOCAL" -> "Pro Local";
        case "PRO" -> "Pro";
        default -> "Gratuit";
    } : "Gratuit";
    String msg = "Bonjour " + v.getDisplayName() + " ! Votre plan BOLA a été mis à jour : " + planName +
        (v.getSubscriptionExpiresAt() != null ? ". Valable jusqu'au " + v.getSubscriptionExpiresAt() + "." : ".") +
        " Connectez-vous sur BOLA pour profiter de vos avantages.";
    String url = "https://wa.me/" + v.getPhone() + "?text=" + 
        java.net.URLEncoder.encode(msg, java.nio.charset.StandardCharsets.UTF_8);
    return "redirect:" + url;
}

@GetMapping("/admin/vendors/{id}/notify-expiry")
public String notifyExpiry(@PathVariable Long id) {
    VendorUser v = vendorUserRepository.findById(id).orElseThrow();
    String planName = v.getPlan() != null ? switch(v.getPlan().name()) {
        case "PREMIUM" -> "Premium";
        case "PRO_LOCAL" -> "Pro Local";
        case "PRO" -> "Pro";
        default -> "Gratuit";
    } : "Gratuit";
    String msg = "Bonjour " + v.getDisplayName() + " ! " +
        (v.getSubscriptionExpiresAt() != null ?
            "Votre abonnement BOLA (" + planName + ") expire le " + v.getSubscriptionExpiresAt() + ". Renouvelez vite !" :
            "Pensez à activer ou renouveler votre abonnement BOLA.");
    String url = "https://wa.me/" + v.getPhone() + "?text=" +
        java.net.URLEncoder.encode(msg, java.nio.charset.StandardCharsets.UTF_8);
    return "redirect:" + url;
}
// ───  AdminController.java après deleteCountry ───────────────

    @PostMapping("/admin/countries/{id}/tax")
    public String updateCountryTax(@PathVariable Long id,
                                   @RequestParam(defaultValue = "0") int customsTaxPercent,
                                   RedirectAttributes ra) {
        countryRepository.findById(id).ifPresent(c -> {
            c.setCustomsTaxPercent(Math.max(0, Math.min(100, customsTaxPercent)));
            countryRepository.save(c);
        });
        ra.addFlashAttribute("flashOk", "Taxe mise à jour.");
        return "redirect:/admin/countries";
    }

    @PostMapping("/admin/countries/import-africa")
    public String importAfricanCountries(RedirectAttributes ra) {
        // Les 54 pays d'Afrique avec drapeaux emoji
        Object[][] africa = {
            {"DZ","Algérie","🇩🇿"},{"AO","Angola","🇦🇴"},{"BJ","Bénin","🇧🇯"},
            {"BW","Botswana","🇧🇼"},{"BF","Burkina Faso","🇧🇫"},{"BI","Burundi","🇧🇮"},
            {"CV","Cap-Vert","🇨🇻"},{"CM","Cameroun","🇨🇲"},{"CF","Centrafrique","🇨🇫"},
            {"TD","Tchad","🇹🇩"},{"KM","Comores","🇰🇲"},{"CG","Congo","🇨🇬"},
            {"CD","RD Congo","🇨🇩"},{"CI","Côte d'Ivoire","🇨🇮"},{"DJ","Djibouti","🇩🇯"},
            {"EG","Égypte","🇪🇬"},{"GQ","Guinée Équatoriale","🇬🇶"},{"ER","Érythrée","🇪🇷"},
            {"SZ","Eswatini","🇸🇿"},{"ET","Éthiopie","🇪🇹"},{"GA","Gabon","🇬🇦"},
            {"GM","Gambie","🇬🇲"},{"GH","Ghana","🇬🇭"},{"GN","Guinée","🇬🇳"},
            {"GW","Guinée-Bissau","🇬🇼"},{"KE","Kenya","🇰🇪"},{"LS","Lesotho","🇱🇸"},
            {"LR","Liberia","🇱🇷"},{"LY","Libye","🇱🇾"},{"MG","Madagascar","🇲🇬"},
            {"MW","Malawi","🇲🇼"},{"ML","Mali","🇲🇱"},{"MR","Mauritanie","🇲🇷"},
            {"MU","Maurice","🇲🇺"},{"MA","Maroc","🇲🇦"},{"MZ","Mozambique","🇲🇿"},
            {"NA","Namibie","🇳🇦"},{"NE","Niger","🇳🇪"},{"NG","Nigeria","🇳🇬"},
            {"RW","Rwanda","🇷🇼"},{"ST","São Tomé-et-Príncipe","🇸🇹"},{"SN","Sénégal","🇸🇳"},
            {"SC","Seychelles","🇸🇨"},{"SL","Sierra Leone","🇸🇱"},{"SO","Somalie","🇸🇴"},
            {"ZA","Afrique du Sud","🇿🇦"},{"SS","Soudan du Sud","🇸🇸"},{"SD","Soudan","🇸🇩"},
            {"TZ","Tanzanie","🇹🇿"},{"TG","Togo","🇹🇬"},{"TN","Tunisie","🇹🇳"},
            {"UG","Ouganda","🇺🇬"},{"ZM","Zambie","🇿🇲"},{"ZW","Zimbabwe","🇿🇼"}
        };

        int added = 0;
        for (Object[] row : africa) {
            String code = (String) row[0];
            // Ne pas dupliquer si déjà existant
            if (countryRepository.findByCode(code).isEmpty()) {
                com.bolas.ecommerce.model.Country c = new com.bolas.ecommerce.model.Country();
                c.setCode(code);
                c.setName((String) row[1]);
                c.setFlag((String) row[2]);
                c.setCustomsTaxPercent(0);
                c.setActive(false); // inactif par défaut, tu actives manuellement
                countryRepository.save(c);
                added++;
            }
        }
        ra.addFlashAttribute("flashOk", added + " pays africains importés (inactifs par défaut). Active ceux que tu veux desservir.");
        return "redirect:/admin/countries";
    }

    // ─── Gestion abonnements en attente ──────────────────────────────────────

    @GetMapping("/admin/subscriptions")
    @Transactional(readOnly = true)
    public String subscriptionsPage(Model model) {
        model.addAttribute("pageTitle", "Abonnements en attente — Admin");
        model.addAttribute("pendingList", vendorUserRepository.findPendingSubscriptions());
        model.addAttribute("pendingCount", vendorUserRepository.countPendingSubscriptions());
        return "admin/subscriptions";
    }

    /**
     * Activer un abonnement : applique le plan demandé, définit une durée de 30 jours,
     * vide les champs pending et notifie le vendeur.
     */
    @PostMapping("/admin/subscriptions/{vendorId}/activate")
    @Transactional
    public String activateSubscription(@PathVariable Long vendorId,
                                       @RequestParam(defaultValue = "30") int durationDays,
                                       RedirectAttributes ra) {
        VendorUser vendor = vendorUserRepository.findById(vendorId).orElse(null);
        if (vendor == null) {
            ra.addFlashAttribute("flashError", "Vendeur introuvable.");
            return "redirect:/admin/subscriptions";
        }
        if (!vendor.hasPendingPlan()) {
            ra.addFlashAttribute("flashError", "Ce vendeur n'a pas de demande en attente.");
            return "redirect:/admin/subscriptions";
        }

        VendorPlan plan;
        try {
            plan = VendorPlan.valueOf(vendor.getPendingPlan().toUpperCase());
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("flashError", "Plan invalide : " + vendor.getPendingPlan());
            return "redirect:/admin/subscriptions";
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        vendor.setPlan(plan);
        vendor.setSubscriptionStartsAt(now);
        vendor.setSubscriptionExpiresAt(now.plusDays(durationDays));
        vendor.setPendingPlan(null);
        vendor.setPendingPaymentMethod(null);
        vendor.setPendingPlanRequestedAt(null);
        vendorUserRepository.save(vendor);

        log.info("✅ Admin: abonnement {} activé pour vendeur {} ({}j)", plan, vendor.getId(), durationDays);
        ra.addFlashAttribute("flashOk",
                "Abonnement " + plan.name() + " activé pour " + vendor.getDisplayName() + " (" + durationDays + " jours).");
        return "redirect:/admin/subscriptions";
    }

    /**
     * Refuser une demande d'abonnement : vide les champs pending, garde le plan actuel.
     */
    @PostMapping("/admin/subscriptions/{vendorId}/refuse")
    @Transactional
    public String refuseSubscription(@PathVariable Long vendorId, RedirectAttributes ra) {
        VendorUser vendor = vendorUserRepository.findById(vendorId).orElse(null);
        if (vendor == null) {
            ra.addFlashAttribute("flashError", "Vendeur introuvable.");
            return "redirect:/admin/subscriptions";
        }
        vendor.setPendingPlan(null);
        vendor.setPendingPaymentMethod(null);
        vendor.setPendingPlanRequestedAt(null);
        vendorUserRepository.save(vendor);

        log.info("❌ Admin: demande d'abonnement refusée pour vendeur {}", vendor.getId());
        ra.addFlashAttribute("flashOk", "Demande de " + vendor.getDisplayName() + " refusée.");
        return "redirect:/admin/subscriptions";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  🛡️ ALERTES FRAUDE — TrustScore Vendeurs
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/admin/fraud-alerts")
    public String fraudAlerts(Model model) {
        model.addAttribute("pageTitle", "Alertes Fraude — Admin BOLA");
        try {
            var alerts = trustScoreService.getFraudAlerts();
            model.addAttribute("fraudAlerts", alerts);
            model.addAttribute("alertCount", alerts.size());
        } catch (Exception e) {
            log.error("Erreur chargement alertes fraude", e);
            model.addAttribute("fraudAlerts", java.util.List.of());
            model.addAttribute("alertCount", 0);
        }
        return "admin/fraud-alerts";
    }

    @PostMapping("/admin/fraud-alerts/{vendorId}/resolve")
    public String resolveFraudAlert(@PathVariable Long vendorId, RedirectAttributes ra) {
        trustScoreService.resolveFlag(vendorId);
        ra.addFlashAttribute("flashOk", "Alerte résolue pour le vendeur #" + vendorId);
        return "redirect:/admin/fraud-alerts";
    }

    @PostMapping("/admin/fraud-alerts/{vendorId}/suspend")
    public String suspendFraudVendor(@PathVariable Long vendorId,
                                      @RequestParam(required = false) String reason,
                                      RedirectAttributes ra) {
        VendorUser vendor = vendorUserRepository.findById(vendorId).orElse(null);
        if (vendor == null) {
            ra.addFlashAttribute("flashError", "Vendeur introuvable.");
            return "redirect:/admin/fraud-alerts";
        }
        vendor.setVendorStatus(VendorStatus.SUSPENDED);
        vendor.setActive(false);
        vendor.setSuspensionReason(reason != null ? reason : "Suspicion de fraude détectée par le système IA");
        vendor.setSoftSuspend(false);
        vendorUserRepository.save(vendor);
        trustScoreService.resolveFlag(vendorId);
        log.info("🚨 Admin: vendeur {} suspendu pour fraude", vendorId);
        ra.addFlashAttribute("flashOk", vendor.getDisplayName() + " suspendu pour fraude.");
        return "redirect:/admin/fraud-alerts";
    }

    @PostMapping("/admin/trust-scores/recalculate")
    public String recalculateTrustScores(RedirectAttributes ra) {
        int count = trustScoreService.recalculateAll();
        ra.addFlashAttribute("flashOk", "TrustScores recalculés pour " + count + " vendeurs.");
        return "redirect:/admin/fraud-alerts";
    }

}
