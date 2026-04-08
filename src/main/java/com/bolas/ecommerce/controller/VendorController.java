package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.model.*;
import com.bolas.ecommerce.repository.*;
import com.bolas.ecommerce.service.IdDocumentVerificationService;
import com.bolas.ecommerce.service.InputSanitizerService;
import com.bolas.ecommerce.service.ImageUploadService;
import com.bolas.ecommerce.service.WhatsAppNotificationService;import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vendor")
public class VendorController {

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
                            CourierApplicationRepository courierApplicationRepository) {
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
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private VendorUser currentVendor(HttpSession session) {
        Object obj = session.getAttribute(SESSION_KEY);
        if (obj instanceof VendorUser v) return v;
        return null;
    }

    private String requireVendor(HttpSession session) {
        if (currentVendor(session) == null) return "redirect:/vendor/login";
        return null;
    }

    /** Catégories autorisées pour ce vendeur */
    private List<Category> allowedCategories(VendorUser vendor) {
        List<Category> cats = vendorCategoryRepository.findCategoriesByVendor(vendor);
        // Si aucune restriction → toutes les catégories (rétro-compatible)
        return cats.isEmpty() ? categoryRepository.findAll() : cats;
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
        session.setAttribute(SESSION_KEY, v);
        return "redirect:/vendor/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
        return "redirect:/vendor/login";
    }

    // ─── Inscription publique ─────────────────────────────────────────────────

    @GetMapping("/register")
    public String registerPage(HttpSession session, Model model) {
        if (currentVendor(session) != null) return "redirect:/vendor/dashboard";
        model.addAttribute("pageTitle", "Ouvrir ma boutique — BOLA");
        model.addAttribute("categories", categoryRepository.findAll());
        return "vendor/register";
    }

    @PostMapping("/register")
    public String registerSubmit(@RequestParam String shopName,
                                 @RequestParam String username,
                                 @RequestParam String phone,
                                 @RequestParam(required = false) String email,
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
        String cleanEmail       = (email != null && !email.isBlank()) ? sanitizer.sanitizeText(email) : null;
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
        if (cleanEmail != null && vendorUserRepository.findByEmail(cleanEmail).isPresent()) {
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

        String waLink = whatsAppService.buildVendorRegistrationLink(
                v.getShopName(), v.getUsername(), v.getPhone(),
                catNames, v.getRequestedNiche());

        ra.addFlashAttribute("flashOk",
                "✅ Demande envoyée ! Notre équipe validera votre boutique sous 24h.");
        ra.addFlashAttribute("waNotifLink", waLink);
        return "redirect:/vendor/register";
    }

    // ─── Dashboard vendeur ────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;

        VendorUser vendor = currentVendor(session);
        vendor = vendorUserRepository.findById(vendor.getId()).orElse(vendor);
        session.setAttribute(SESSION_KEY, vendor);

        long productCount   = productRepository.countByVendor(vendor);
        long pendingOrders  = orderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.PENDING).size();
        long confirmedOrders = orderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.CONFIRMED).size();

        model.addAttribute("pageTitle", "Mon espace vendeur — BOLA");
        model.addAttribute("vendor",    vendor);
        model.addAttribute("productCount",    productCount);
        model.addAttribute("pendingOrders",   pendingOrders);
        model.addAttribute("confirmedOrders", confirmedOrders);
        model.addAttribute("gratuitLimit",    GRATUIT_LIMIT);
        model.addAttribute("allowedCategories", allowedCategories(vendor));
        model.addAttribute("products",
                productRepository.findByVendor(vendor).stream()
                        .limit(5).toList());
        return "vendor/dashboard";
    }

    // ─── Commandes vendeur ────────────────────────────────────────────────────

    @GetMapping("/orders")
    public String orders(HttpSession session, Model model) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;

        VendorUser vendor = currentVendor(session);
        List<CustomerOrder> toProcess =
                orderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.CONFIRMED);
        List<CustomerOrder> done =
                orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.READY);

        model.addAttribute("pageTitle", "Mes commandes — BOLA Vendeur");
        model.addAttribute("vendor",    vendor);
        model.addAttribute("toProcess", toProcess);
        model.addAttribute("done",      done);
        return "vendor/orders";
    }

    @PostMapping("/orders/{id}/ready")
    public String markReady(@PathVariable Long id,
                            HttpSession session,
                            RedirectAttributes ra) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;

        CustomerOrder order = orderRepository.findById(id).orElseThrow();
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            ra.addFlashAttribute("flashError", "Cette commande ne peut pas être marquée prête.");
            return "redirect:/vendor/orders";
        }
        order.setStatus(OrderStatus.READY);
        orderRepository.save(order);
        ra.addFlashAttribute("flashOk", "Commande marquée comme prête !");
        return "redirect:/vendor/orders";
    }

    // ─── Produits vendeur ─────────────────────────────────────────────────────

    @GetMapping("/products")
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
                              @RequestParam Long categoryId,
                              @RequestParam(required = false) String imageUrl,
                              @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                              @RequestParam(defaultValue = "true") boolean available,
                              @RequestParam(defaultValue = "true") boolean deliveryAvailable,
                              @RequestParam(defaultValue = "0") long deliveryPriceCfa,
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

        // Vérifier que le vendeur a accès à cette catégorie
        List<Category> allowed = allowedCategories(vendor);
        if (!allowed.isEmpty() && allowed.stream().noneMatch(c -> c.getId().equals(categoryId))) {
            ra.addFlashAttribute("flashError",
                    "Vous n'avez pas accès à cette catégorie. Demandez l'accès depuis votre dashboard.");
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

        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                p.setImageUrl(imageUploadService.store(imageFile));
            } else if (imageUrl != null && !imageUrl.isBlank()) {
                p.setImageUrl(imageUrl.trim());
            }
        } catch (IllegalArgumentException | IOException e) {
            ra.addFlashAttribute("flashError", "Échec upload image : " + e.getMessage());
            return "redirect:/vendor/products/add";
        }

        productRepository.save(p);
        ra.addFlashAttribute("flashOk", "Produit \"" + p.getName() + "\" ajouté avec succès !");
        return "redirect:/vendor/products";
    }

    @GetMapping("/products/{id}/edit")
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
                                @RequestParam Long categoryId,
                                @RequestParam(required = false) String imageUrl,
                                @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                @RequestParam(defaultValue = "true") boolean available,
                                @RequestParam(defaultValue = "true") boolean deliveryAvailable,
                                @RequestParam(defaultValue = "0") long deliveryPriceCfa,
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

        p.setName(name.trim());
        p.setDescription(description);
        p.setPriceCfa(priceCfa);
        p.setPromoPriceCfa(promoPriceCfa != null && promoPriceCfa > 0 ? promoPriceCfa : null);
        p.setCategory(category);
        p.setAvailable(available);
        p.setDeliveryAvailable(deliveryAvailable);
        p.setDeliveryPriceCfa(deliveryPriceCfa);

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
    public String messagesInbox(HttpSession session, Model model) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;

        VendorUser vendor = currentVendor(session);
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

        ra.addFlashAttribute("flashOk", "Demande envoyée ! L'admin validera ce livreur sous 24h.");
        return "redirect:/vendor/couriers";
    }
}
