package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.model.*;
import com.bolas.ecommerce.repository.*;
import com.bolas.ecommerce.service.ImageUploadService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/vendor")
public class VendorController {

    private static final String SESSION_KEY   = "BOLAS_VENDOR";
    private static final int    GRATUIT_LIMIT = 10;

    private final CustomerOrderRepository orderRepository;
    private final VendorUserRepository    vendorUserRepository;
    private final ProductRepository       productRepository;
    private final CategoryRepository      categoryRepository;
    private final PasswordEncoder         passwordEncoder;
    private final ImageUploadService      imageUploadService;

    public VendorController(CustomerOrderRepository orderRepository,
                            VendorUserRepository vendorUserRepository,
                            ProductRepository productRepository,
                            CategoryRepository categoryRepository,
                            PasswordEncoder passwordEncoder,
                            ImageUploadService imageUploadService) {
        this.orderRepository      = orderRepository;
        this.vendorUserRepository = vendorUserRepository;
        this.productRepository    = productRepository;
        this.categoryRepository   = categoryRepository;
        this.passwordEncoder      = passwordEncoder;
        this.imageUploadService   = imageUploadService;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Récupère le vendeur en session, ou null s'il n'est pas connecté. */
    private VendorUser currentVendor(HttpSession session) {
        Object obj = session.getAttribute(SESSION_KEY);
        if (obj instanceof VendorUser v) return v;
        return null;
    }

    /** Vérifie la session vendeur et redirige vers login si absent. */
    private String requireVendor(HttpSession session) {
        if (currentVendor(session) == null) return "redirect:/vendor/login";
        return null; // OK
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
        return "vendor/register";
    }

    @PostMapping("/register")
    public String registerSubmit(@RequestParam String shopName,
                                 @RequestParam String username,
                                 @RequestParam String phone,
                                 @RequestParam(required = false) String email,
                                 @RequestParam String password,
                                 @RequestParam(required = false) String shopDescription,
                                 RedirectAttributes ra) {
        if (password == null || password.length() < 6) {
            ra.addFlashAttribute("flashError", "Le mot de passe doit faire au moins 6 caractères.");
            return "redirect:/vendor/register";
        }
        if (vendorUserRepository.findByUsername(username.trim()).isPresent()) {
            ra.addFlashAttribute("flashError", "Ce nom d'utilisateur est déjà pris. Choisissez-en un autre.");
            return "redirect:/vendor/register";
        }
        if (email != null && !email.isBlank() &&
                vendorUserRepository.findByEmail(email.trim()).isPresent()) {
            ra.addFlashAttribute("flashError", "Cet email est déjà utilisé.");
            return "redirect:/vendor/register";
        }

        VendorUser v = new VendorUser();
        v.setUsername(username.trim());
        v.setShopName(shopName.trim());
        v.setPhone(phone.trim());
        v.setEmail(email != null ? email.trim() : null);
        v.setShopDescription(shopDescription != null ? shopDescription.trim() : null);
        v.setPasswordHash(passwordEncoder.encode(password));
        v.setActive(false);
        v.setVendorStatus(VendorStatus.PENDING);
        v.setPlan(VendorPlan.GRATUIT);
        vendorUserRepository.save(v);

        ra.addFlashAttribute("flashOk",
                "✅ Demande envoyée ! Notre équipe validera votre boutique sous 24h. " +
                "Vous recevrez une confirmation par WhatsApp ou email.");
        return "redirect:/vendor/register";
    }

    // ─── Dashboard vendeur ────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        String redirect = requireVendor(session);
        if (redirect != null) return redirect;

        VendorUser vendor = currentVendor(session);
        // Rafraîchir depuis BDD (données à jour)
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
                    "Limite atteinte (10 produits max en Pack Débutant). Passez au Pack Pro pour en ajouter davantage.");
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
        model.addAttribute("categories", categoryRepository.findAll());
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

        // Vérification limite
        if (vendor.getPlan() == VendorPlan.GRATUIT &&
                productRepository.countByVendor(vendor) >= GRATUIT_LIMIT) {
            ra.addFlashAttribute("flashError",
                    "Limite de 10 produits atteinte. Passez au Pack Pro pour continuer.");
            return "redirect:/vendor/products";
        }

        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category == null) {
            ra.addFlashAttribute("flashError", "Catégorie invalide.");
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

        // Image
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
        model.addAttribute("categories", categoryRepository.findAll());
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
}
