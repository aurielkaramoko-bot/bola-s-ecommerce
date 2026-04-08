package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.model.*;
import com.bolas.ecommerce.repository.*;
import com.bolas.ecommerce.service.WhatsAppNotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;

/**
 * Contrôleur pour les avis produit et les signalements.
 */
@Controller
public class ReviewReportController {

    private final ReviewRepository reviewRepository;
    private final ReportRepository reportRepository;
    private final ProductRepository productRepository;
    private final VendorUserRepository vendorUserRepository;
    private final WhatsAppNotificationService whatsAppService;

    public ReviewReportController(ReviewRepository reviewRepository,
                                   ReportRepository reportRepository,
                                   ProductRepository productRepository,
                                   VendorUserRepository vendorUserRepository,
                                   WhatsAppNotificationService whatsAppService) {
        this.reviewRepository = reviewRepository;
        this.reportRepository = reportRepository;
        this.productRepository = productRepository;
        this.vendorUserRepository = vendorUserRepository;
        this.whatsAppService = whatsAppService;
    }

    /** Soumettre un avis sur un produit */
    @PostMapping("/products/{id}/review")
    public String submitReview(@PathVariable Long id,
                               @RequestParam String reviewerName,
                               @RequestParam(required = false) String reviewerEmail,
                               @RequestParam int rating,
                               @RequestParam(required = false) String comment,
                               RedirectAttributes ra) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            ra.addFlashAttribute("flashError", "Produit introuvable.");
            return "redirect:/products";
        }

        if (rating < 1 || rating > 5) {
            ra.addFlashAttribute("flashError", "Note invalide.");
            return "redirect:/products/" + id;
        }

        Review review = new Review();
        review.setProduct(product);
        review.setReviewerName(reviewerName != null ? reviewerName.trim() : "Anonyme");
        review.setReviewerEmail(reviewerEmail != null ? reviewerEmail.trim() : null);
        review.setRating(rating);
        review.setComment(comment != null ? comment.trim() : null);
        review.setCreatedAt(Instant.now());
        review.setApproved(false); // Modération admin
        reviewRepository.save(review);

        ra.addFlashAttribute("flashOk", "Merci pour votre avis ! Il sera publié après modération.");
        return "redirect:/products/" + id;
    }

    /** Signaler un produit ou un vendeur */
    @PostMapping("/report")
    public String submitReport(@RequestParam String targetType,
                               @RequestParam Long targetId,
                               @RequestParam(required = false) String reporterEmail,
                               @RequestParam String reason,
                               RedirectAttributes ra) {
        if (reason == null || reason.isBlank()) {
            ra.addFlashAttribute("flashError", "Veuillez préciser la raison du signalement.");
            return "redirect:/products";
        }

        String targetName = "?";
        if ("PRODUCT".equals(targetType)) {
            targetName = productRepository.findById(targetId)
                    .map(Product::getName).orElse("Produit #" + targetId);
        } else if ("VENDOR".equals(targetType)) {
            targetName = vendorUserRepository.findById(targetId)
                    .map(VendorUser::getDisplayName).orElse("Vendeur #" + targetId);
        }

        Report report = new Report();
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setTargetName(targetName);
        report.setReporterEmail(reporterEmail != null ? reporterEmail.trim() : null);
        report.setReason(reason.trim());
        report.setCreatedAt(Instant.now());
        report.setResolved(false);
        reportRepository.save(report);

        // Lien WhatsApp admin
        String waLink = whatsAppService.buildReportLink(targetType, targetName,
                reporterEmail != null ? reporterEmail : "non fourni", reason);

        ra.addFlashAttribute("flashOk", "Signalement envoyé. Notre équipe va examiner la situation.");
        ra.addFlashAttribute("waReportLink", waLink);

        if ("PRODUCT".equals(targetType)) {
            return "redirect:/products/" + targetId;
        }
        return "redirect:/products";
    }
}
