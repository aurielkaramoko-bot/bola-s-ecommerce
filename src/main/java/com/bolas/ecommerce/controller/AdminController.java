package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.dto.CourierUpdateDto;
import com.bolas.ecommerce.dto.NewOrderDto;
import com.bolas.ecommerce.model.Category;
import com.bolas.ecommerce.model.CustomerOrder;
import com.bolas.ecommerce.model.DeliveryOption;
import com.bolas.ecommerce.model.OrderLine;
import com.bolas.ecommerce.model.OrderStatus;
import com.bolas.ecommerce.model.Product;
import com.bolas.ecommerce.repository.CategoryRepository;
import com.bolas.ecommerce.repository.CustomerOrderRepository;
import com.bolas.ecommerce.repository.OrderLineRepository;
import com.bolas.ecommerce.repository.ProductRepository;
import com.bolas.ecommerce.service.AuditLogService;
import com.bolas.ecommerce.service.CategoryCoverImageUrlService;
import com.bolas.ecommerce.service.CategoryCoverImageUrlService.Resolution;
import com.bolas.ecommerce.service.CategoryCoverImageUrlService.ResolutionKind;
import com.bolas.ecommerce.service.ImageUploadService;
import com.bolas.ecommerce.service.InputSanitizerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value; // IMPORT IMPORTANT
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
public class AdminController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderLineRepository orderLineRepository;
    private final ImageUploadService imageUploadService;
    private final CategoryCoverImageUrlService categoryCoverImageUrlService;
    private final InputSanitizerService inputSanitizerService;
    private final AuditLogService auditLogService;

    // --- ICI : RÉCUPÉRATION DE TA CLÉ API DEPUIS TON PC ---
    @Value("${google.maps.api.key}")
    private String googleMapsApiKey;

    @Value("${bolas.shop.latitude:5.3600}")
    private double shopLatitude;

    @Value("${bolas.shop.longitude:-3.9903}")
    private double shopLongitude;

    public AdminController(ProductRepository productRepository,
                           CategoryRepository categoryRepository,
                           CustomerOrderRepository customerOrderRepository,
                           OrderLineRepository orderLineRepository,
                           ImageUploadService imageUploadService,
                           CategoryCoverImageUrlService categoryCoverImageUrlService,
                           InputSanitizerService inputSanitizerService,
                           AuditLogService auditLogService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.orderLineRepository = orderLineRepository;
        this.imageUploadService = imageUploadService;
        this.categoryCoverImageUrlService = categoryCoverImageUrlService;
        this.inputSanitizerService = inputSanitizerService;
        this.auditLogService = auditLogService;
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
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Tableau de bord — Bola's");
        model.addAttribute("productCount", productRepository.count());
        model.addAttribute("categoryCount", categoryRepository.count());
        model.addAttribute("orderCount", customerOrderRepository.count());
        model.addAttribute("recentOrders", customerOrderRepository.findTop10ByOrderByCreatedAtDesc());

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

    @GetMapping("/admin/products")
    public String products(Model model) {
        model.addAttribute("pageTitle", "Produits — Admin Bola's");
        model.addAttribute("products", productRepository.findAll());
        return "admin/products";
    }

    @GetMapping("/admin/products/new")
    public String newProduct(Model model) {
        model.addAttribute("pageTitle", "Nouveau produit — Bola's");
        Product p = new Product();
        p.setAvailable(true);
        p.setDeliveryAvailable(true);
        p.setFeatured(false);
        p.setDeliveryPriceCfa(0L);
        p.setCategory(new Category());
        model.addAttribute("product", p);
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/product-form";
    }

    @GetMapping("/admin/products/{id}/edit")
    public String editProduct(@PathVariable Long id, Model model) {
        Product p = productRepository.findById(id).orElseThrow();
        model.addAttribute("pageTitle", "Modifier le produit — Bola's");
        model.addAttribute("product", p);
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/product-form";
    }

    @PostMapping("/admin/products")
    public String saveProduct(@Valid @ModelAttribute Product form,
                              BindingResult bindingResult,
                              @RequestParam(value = "availableChecked", required = false) String availableChecked,
                              @RequestParam(value = "deliveryAvailableChecked", required = false) String deliveryChecked,
                              @RequestParam(value = "featuredChecked", required = false) String featuredChecked,
                              @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                              @RequestParam(value = "videoFile", required = false) MultipartFile videoFile,
                              @RequestParam(value = "removeVideo", required = false) String removeVideo,
                              Model model) {
        form.setAvailable("true".equals(availableChecked));
        form.setDeliveryAvailable("true".equals(deliveryChecked));
        form.setFeatured("true".equals(featuredChecked));
        form.setName(inputSanitizerService.sanitizeText(form.getName()));
        form.setDescription(inputSanitizerService.sanitizeText(form.getDescription()));
        form.setImageUrl(inputSanitizerService.sanitizeText(form.getImageUrl()));

        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", form.getId() != null ? "Modifier le produit — Bola's" : "Nouveau produit — Bola's");
            model.addAttribute("product", form);
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("flashError", "Vérifiez les champs du produit.");
            return "admin/product-form";
        }
        if (form.getCategory() == null || form.getCategory().getId() == null) {
            model.addAttribute("pageTitle", form.getId() != null ? "Modifier le produit — Bola's" : "Nouveau produit — Bola's");
            model.addAttribute("product", form);
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("flashError", "Choisissez une catégorie.");
            return "admin/product-form";
        }

        Category category = categoryRepository.findById(form.getCategory().getId()).orElseThrow();
        form.setCategory(category);

        try {
            // --- Image ---
            if (imageFile != null && !imageFile.isEmpty()) {
                form.setImageUrl(imageUploadService.store(imageFile));
            } else if (form.getId() != null) {
                Product existing = productRepository.findById(form.getId()).orElseThrow();
                if (form.getImageUrl() == null || form.getImageUrl().isBlank()) {
                    form.setImageUrl(existing.getImageUrl());
                }
            }
            // --- Vidéo ---
            if ("true".equals(removeVideo)) {
                form.setVideoUrl(null);
            } else if (videoFile != null && !videoFile.isEmpty()) {
                form.setVideoUrl(imageUploadService.storeVideo(videoFile));
            } else if (form.getId() != null && (form.getVideoUrl() == null || form.getVideoUrl().isBlank())) {
                Product existing = productRepository.findById(form.getId()).orElseThrow();
                form.setVideoUrl(existing.getVideoUrl());
            }
        } catch (IllegalArgumentException e) {
            model.addAttribute("pageTitle", form.getId() != null ? "Modifier le produit — Bola's" : "Nouveau produit — Bola's");
            model.addAttribute("product", form);
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("flashError", e.getMessage());
            return "admin/product-form";
        } catch (IOException e) {
            model.addAttribute("pageTitle", form.getId() != null ? "Modifier le produit — Bola's" : "Nouveau produit — Bola's");
            model.addAttribute("product", form);
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("flashError", "Échec de l'enregistrement du fichier.");
            return "admin/product-form";
        }

        productRepository.save(form);
        boolean isNew = form.getId() == null;
        Product saved = productRepository.save(form);
        if (isNew) {
            auditLogService.productCreated(saved.getId(), saved.getName());
        } else {
            auditLogService.productUpdated(saved.getId(), saved.getName());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/admin/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            redirectAttributes.addFlashAttribute("flashError", "Produit introuvable.");
            return "redirect:/admin/products";
        }
        long linkedOrderLines = orderLineRepository.countByProduct_Id(id);
        if (linkedOrderLines > 0 && product.isAvailable()) {
            redirectAttributes.addFlashAttribute("flashError",
                    "Impossible de supprimer ce produit : il figure sur au moins une commande (historique). "
                            + "Décochez « Produit disponible » puis supprimez à nouveau.");
            return "redirect:/admin/products";
        }
        if (linkedOrderLines > 0) {
            orderLineRepository.deleteByProduct_Id(id);
        }
        String productName = product.getName();
        productRepository.deleteById(id);
        auditLogService.productDeleted(id, productName);
        redirectAttributes.addFlashAttribute("flashOk", "Produit supprimé.");
        return "redirect:/admin/products";
    }

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

    @GetMapping("/admin/orders")
    public String orders(Model model) {
        model.addAttribute("pageTitle", "Commandes — Admin Bola's");
        model.addAttribute("orders", customerOrderRepository.findAllByOrderByCreatedAtDesc());
        model.addAttribute("newOrder", new NewOrderDto());
        model.addAttribute("products", productRepository.findByAvailableTrue());
        return "admin/orders";
    }

    @PostMapping("/admin/orders/new")
    public String createOrder(@Valid @ModelAttribute("newOrder") NewOrderDto dto,
                              BindingResult br,
                              @RequestParam(required = false) Long productId,
                              RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("flashError", "Vérifiez les champs de la commande.");
            return "redirect:/admin/orders";
        }
        CustomerOrder order = new CustomerOrder();
        order.setTrackingNumber("BOL-" + UUID.randomUUID().toString().replace("-","").substring(0,8).toUpperCase());
        order.setCustomerName(dto.getCustomerName());
        order.setCustomerPhone(dto.getCustomerPhone());
        order.setCustomerAddress(dto.getCustomerAddress());
        order.setDeliveryOption(dto.getDeliveryOption() != null ? dto.getDeliveryOption() : DeliveryOption.HOME);
        order.setTotalAmountCfa(dto.getTotalAmountCfa());
        order.setDeliveryFeeCfa(dto.getDeliveryFeeCfa());

        if (productId != null) {
            productRepository.findById(productId).ifPresent(p -> {
                OrderLine line = new OrderLine();
                line.setProduct(p);
                line.setQuantity(1);
                line.setUnitPriceCfa(p.getEffectivePriceCfa());
                order.addLine(line);
            });
        }
        customerOrderRepository.save(order);
        auditLogService.orderStatusChanged(order.getId(), order.getTrackingNumber(), "CREATED");
        ra.addFlashAttribute("flashOk", "Commande " + order.getTrackingNumber() + " créée.");
        return "redirect:/admin/orders";
    }

    @PostMapping("/admin/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        CustomerOrder order = customerOrderRepository.findById(id).orElseThrow();
        order.setStatus(status);
        customerOrderRepository.save(order);
        auditLogService.orderStatusChanged(id, order.getTrackingNumber(), status.name());
        return "redirect:/admin/orders";
    }

    @PostMapping("/admin/orders/{id}/delete")
    public String deleteOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        CustomerOrder order = customerOrderRepository.findById(id).orElseThrow();
        String tracking = order.getTrackingNumber();
        customerOrderRepository.deleteById(id);
        auditLogService.orderDeleted(id, tracking);
        redirectAttributes.addFlashAttribute("flashOk", "Commande supprimée.");
        return "redirect:/admin/orders";
    }

    // --- ICI : LA MÉTHODE POUR LA PAGE DE LIVRAISON AVEC LA CLÉ API ---
    @GetMapping("/admin/delivery")
    public String deliveryForm(Model model) {
        model.addAttribute("pageTitle", "Livraison GPS — Admin Bola's");
        model.addAttribute("apiKey", googleMapsApiKey);
        model.addAttribute("shopLatitude", shopLatitude);
        model.addAttribute("shopLongitude", shopLongitude);
        model.addAttribute("courierUpdate", new CourierUpdateDto());
        model.addAttribute("orders", customerOrderRepository.findAllByOrderByCreatedAtDesc());
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
            model.addAttribute("apiKey", googleMapsApiKey);
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
                model.addAttribute("apiKey", googleMapsApiKey);
                model.addAttribute("shopLatitude", shopLatitude);
                model.addAttribute("shopLongitude", shopLongitude);
                model.addAttribute("orders", customerOrderRepository.findAllByOrderByCreatedAtDesc());
                model.addAttribute("flashError", e.getMessage());
                return "admin/delivery-update";
            } catch (IOException e) {
                model.addAttribute("pageTitle", "Livraison GPS — Admin Bola's");
                model.addAttribute("apiKey", googleMapsApiKey);
                model.addAttribute("shopLatitude", shopLatitude);
                model.addAttribute("shopLongitude", shopLongitude);
                model.addAttribute("orders", customerOrderRepository.findAllByOrderByCreatedAtDesc());
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
}