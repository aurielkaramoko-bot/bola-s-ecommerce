package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.model.Customer;
import com.bolas.ecommerce.model.Product;
import com.bolas.ecommerce.model.ProductComment;
import com.bolas.ecommerce.model.VendorUser;
import com.bolas.ecommerce.repository.CustomerOrderRepository;
import com.bolas.ecommerce.repository.ProductCommentRepository;
import com.bolas.ecommerce.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class ProductCommentController {

    private final ProductCommentRepository commentRepo;
    private final ProductRepository productRepo;
    private final CustomerOrderRepository orderRepo;

    public ProductCommentController(ProductCommentRepository commentRepo,
                                    ProductRepository productRepo,
                                    CustomerOrderRepository orderRepo) {
        this.commentRepo = commentRepo;
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
    }

    /** GET /api/comments/{productId} — liste racines + count réponses */
    @GetMapping("/{productId}")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> list(@PathVariable Long productId) {
        Product p = productRepo.findById(productId).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        List<ProductComment> roots = commentRepo.findRootByProduct(p);
        long total = commentRepo.countByProduct(p);
        return ResponseEntity.ok(Map.of("comments", roots, "total", total));
    }

    /** GET /api/comments/{productId}/replies/{parentId} */
    @GetMapping("/{productId}/replies/{parentId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ProductComment>> replies(@PathVariable Long productId,
                                                         @PathVariable Long parentId) {
        return ResponseEntity.ok(commentRepo.findReplies(parentId));
    }

    /** POST /api/comments/{productId} — poster un commentaire */
    @PostMapping("/{productId}")
    @Transactional
    public ResponseEntity<ProductComment> post(@PathVariable Long productId,
                                                @RequestBody Map<String, String> body,
                                                HttpSession session) {
        Customer customer = (Customer) session.getAttribute("BOLAS_CUSTOMER");
        VendorUser vendor = (VendorUser) session.getAttribute("BOLAS_VENDOR");

        if (customer == null && vendor == null) {
            return ResponseEntity.status(401).build();
        }

        Product product = productRepo.findById(productId).orElse(null);
        if (product == null) return ResponseEntity.notFound().build();

        String text = body.getOrDefault("text", "").trim();
        if (text.isBlank() || text.length() > 1000) return ResponseEntity.badRequest().build();

        ProductComment c = new ProductComment();
        c.setProduct(product);
        c.setText(text);

        String parentIdStr = body.get("parentId");
        if (parentIdStr != null && !parentIdStr.isBlank()) {
            try { c.setParentId(Long.parseLong(parentIdStr)); } catch (NumberFormatException ignored) {}
        }

        if (customer != null) {
            c.setCustomerId(customer.getId());
            String name = customer.getDisplayName() != null ? customer.getDisplayName()
                        : customer.getFirstName() != null ? customer.getFirstName() : "Client";
            c.setAuthorName(name);
            // Badge achat vérifié : a-t-il commandé ce produit ?
            boolean bought = orderRepo.existsByCustomerPhoneAndProductId(customer.getPhone(), productId);
            c.setVerifiedBuyer(bought);
        } else {
            // Réponse vendeur
            c.setVendorId(vendor.getId());
            c.setAuthorName(vendor.getDisplayName());
        }

        // Photo jointe au commentaire (upload Cloudinary côté front)
        String photoUrl = body.get("photoUrl");
        if (photoUrl != null && !photoUrl.isBlank() && photoUrl.startsWith("https://")) {
            c.setPhotoUrl(photoUrl);
        }

        return ResponseEntity.ok(commentRepo.save(c));
    }

    /** POST /api/comments/{id}/like */
    @PostMapping("/{id}/like")
    @Transactional
    public ResponseEntity<Map<String, Integer>> like(@PathVariable Long id, HttpSession session) {
        Customer customer = (Customer) session.getAttribute("BOLAS_CUSTOMER");
        VendorUser vendor = (VendorUser) session.getAttribute("BOLAS_VENDOR");
        if (customer == null && vendor == null) return ResponseEntity.status(401).build();

        commentRepo.incrementLikes(id);
        ProductComment c = commentRepo.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("likes", c.getLikesCount()));
    }

    /** DELETE /api/comments/{id} — vendeur ou admin */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpSession session) {
        VendorUser vendor = (VendorUser) session.getAttribute("BOLAS_VENDOR");
        if (vendor == null) return ResponseEntity.status(403).build();

        ProductComment c = commentRepo.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        // Vérifier que c'est bien un produit du vendeur
        if (!c.getProduct().getVendor().getId().equals(vendor.getId())) {
            return ResponseEntity.status(403).build();
        }
        c.setDeleted(true);
        commentRepo.save(c);
        return ResponseEntity.ok().build();
    }
}
