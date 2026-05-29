package com.bolas.ecommerce.service;

import com.bolas.ecommerce.model.VendorPlan;
import com.bolas.ecommerce.model.VendorUser;
import com.bolas.ecommerce.repository.ProductRepository;
import com.bolas.ecommerce.repository.VendorUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Vérifie toutes les 60 secondes si des abonnements vendeurs ont expiré.
 * Blocage instantané à la minute exacte + notification WhatsApp.
 */
@Service
public class SubscriptionExpiryService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionExpiryService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH'h'mm");

    private final VendorUserRepository vendorUserRepository;
    private final ProductRepository productRepository;
    private final MetaWhatsAppService metaWhatsApp;

    public SubscriptionExpiryService(VendorUserRepository vendorUserRepository,
                                     ProductRepository productRepository,
                                     MetaWhatsAppService metaWhatsApp) {
        this.vendorUserRepository = vendorUserRepository;
        this.productRepository = productRepository;
        this.metaWhatsApp = metaWhatsApp;
    }

    /** Vérifie toutes les 60 secondes les abonnements expirés — blocage instantané */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void checkExpiredSubscriptions() {
        LocalDateTime now = LocalDateTime.now();

        List<VendorUser> expired = vendorUserRepository.findAll().stream()
                .filter(v -> v.getSubscriptionExpiresAt() != null
                        && v.getSubscriptionExpiresAt().isBefore(now)
                        && v.getPlan() != VendorPlan.GRATUIT
                        && v.isActive())
                .toList();

        if (expired.isEmpty()) return;

        for (VendorUser vendor : expired) {
            String planName = vendor.getPlan().name();
            String planLabel = switch (vendor.getPlan()) {
                case PRO_LOCAL -> "Pro Local";
                case PRO       -> "Pro";
                case PREMIUM   -> "Premium";
                default        -> "Gratuit";
            };

            log.info("⏰ Abonnement expiré pour {} ({}), rétrogradation en GRATUIT",
                    vendor.getDisplayName(), vendor.getSubscriptionExpiresAt());

            // 1. Rétrograder en GRATUIT
            vendor.setPlan(VendorPlan.GRATUIT);

            // 2. Désactiver les produits au-delà de la limite GRATUIT (garder les 10 plus récents)
            var products = productRepository.findByVendorOrderByIdDesc(vendor);
            if (products.size() > 10) {
                products.stream().skip(10).forEach(p -> {
                    p.setAvailable(false);
                    productRepository.save(p);
                });
                log.info("   → {} produits masqués (limite GRATUIT)", products.size() - 10);
            }

            vendorUserRepository.save(vendor);

            // 3. Envoyer WhatsApp au vendeur
            if (vendor.getPhone() != null && !vendor.getPhone().isBlank()) {
                String expDate = vendor.getSubscriptionExpiresAt().format(DATE_FMT);
                String expTime = vendor.getSubscriptionExpiresAt().format(TIME_FMT);
                String msg = String.format(
                    "Votre abonnement BOLA %s a expiré le %s à %s.\n" +
                    "Votre boutique est maintenant en mode GRATUIT (10 produits max).\n" +
                    "Renouvelez sur bola.tg pour retrouver tous vos avantages.",
                    planLabel, expDate, expTime
                );
                metaWhatsApp.sendText(vendor.getPhone(), msg);
            }
        }

        log.info("✅ {} abonnement(s) expiré(s) traité(s)", expired.size());
    }

    /** Rappels avant expiration — 3 jours et 1 jour avant */
    @Scheduled(fixedRate = 3_600_000) // toutes les heures
    @Transactional(readOnly = true)
    public void sendRenewalReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in3days = now.plusDays(3);
        LocalDateTime in1day  = now.plusDays(1);

        vendorUserRepository.findAll().stream()
            .filter(v -> v.getSubscriptionExpiresAt() != null
                    && v.getPlan() != VendorPlan.GRATUIT
                    && v.isActive()
                    && v.getPhone() != null)
            .forEach(v -> {
                LocalDateTime exp = v.getSubscriptionExpiresAt();
                String expDate = exp.format(DATE_FMT);
                String expTime = exp.format(TIME_FMT);

                // Rappel 3 jours avant (fenêtre ±30 min)
                if (exp.isAfter(now.plusDays(2).plusMinutes(30)) && exp.isBefore(in3days.plusMinutes(30))) {
                    metaWhatsApp.sendText(v.getPhone(),
                        "⏰ Rappel BOLA : votre abonnement expire le " + expDate + " à " + expTime + ".\n" +
                        "Renouvelez maintenant sur bola.tg pour ne pas interrompre votre boutique.");
                }
                // Rappel 1 jour avant (fenêtre ±30 min)
                else if (exp.isAfter(now.plusMinutes(30)) && exp.isBefore(in1day.plusMinutes(30))) {
                    metaWhatsApp.sendText(v.getPhone(),
                        "🚨 URGENT — Votre abonnement BOLA expire demain le " + expDate + " à " + expTime + ".\n" +
                        "Renouvelez MAINTENANT sur bola.tg pour éviter la suspension de votre boutique.");
                }
            });
    }
}
