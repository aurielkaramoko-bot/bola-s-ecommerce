package com.bolas.ecommerce.service;

import com.bolas.ecommerce.model.VendorUser;
import com.bolas.ecommerce.repository.VendorUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Optional;
import java.util.Random;

/**
 * Gestion du parrainage vendeur BOLA.
 * Un vendeur génère un code → partage → son filleul s'inscrit avec ?ref=CODE
 * → quand le filleul active un plan payant → parrain gagne +1 mois.
 */
@Service
public class ReferralService {

    private static final Logger log = LoggerFactory.getLogger(ReferralService.class);
    private final VendorUserRepository vendorRepo;

    public ReferralService(VendorUserRepository vendorRepo) {
        this.vendorRepo = vendorRepo;
    }

    /** Génère et sauvegarde un code de parrainage unique pour un vendeur */
    @Transactional
    public String generateAndSave(VendorUser vendor) {
        if (vendor.getReferralCode() != null) return vendor.getReferralCode();
        String base = slugify(vendor.getUsername()).toUpperCase();
        if (base.length() > 4) base = base.substring(0, 4);
        String code;
        int attempts = 0;
        do {
            code = "BOLA-" + base + "-" + randomSuffix();
            attempts++;
        } while (vendorRepo.existsByReferralCode(code) && attempts < 10);
        vendor.setReferralCode(code);
        vendorRepo.save(vendor);
        return code;
    }

    /**
     * Appelé quand un nouveau vendeur active un plan payant.
     * Si il a été parrainé, le parrain gagne +1 mois de bonus.
     */
    @Transactional
    public void processActivation(VendorUser newVendor) {
        if (newVendor.getReferredById() == null) return;
        Optional<VendorUser> parrain = vendorRepo.findById(newVendor.getReferredById());
        parrain.ifPresent(p -> {
            p.setReferralBonusMonths(p.getReferralBonusMonths() + 1);
            vendorRepo.save(p);
            log.info("🎁 Parrainage : vendeur#{} gagne +1 mois (filleul #{})",
                    p.getId(), newVendor.getId());
        });
    }

    /** Associe un filleul à son parrain depuis un code ref */
    @Transactional
    public void linkReferral(VendorUser newVendor, String refCode) {
        if (refCode == null || refCode.isBlank()) return;
        vendorRepo.findByReferralCode(refCode).ifPresent(parrain -> {
            newVendor.setReferredById(parrain.getId());
            log.info("🔗 Parrainage : vendeur#{} parrainé par #{}", newVendor.getId(), parrain.getId());
        });
    }

    private String randomSuffix() {
        return String.format("%04X", new Random().nextInt(0xFFFF));
    }

    private static String slugify(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase()
                .replaceAll("[^a-z0-9]", "").toUpperCase();
    }
}
