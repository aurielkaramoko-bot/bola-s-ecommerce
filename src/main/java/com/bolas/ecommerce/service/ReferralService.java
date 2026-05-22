package com.bolas.ecommerce.service;

import com.bolas.ecommerce.model.VendorPlan;
import com.bolas.ecommerce.model.VendorUser;
import com.bolas.ecommerce.repository.VendorUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Gestion du parrainage vendeur BOLA.
 * Règle : 3 filleuls actifs (plan payant) = +1 mois offert par tranche de 3.
 * Ex: 3 filleuls = +1 mois, 6 filleuls = +2 mois, etc.
 */
@Service
public class ReferralService {

    private static final Logger log = LoggerFactory.getLogger(ReferralService.class);
    /** Nombre de filleuls actifs requis pour gagner 1 mois */
    public static final int FILLEULS_PAR_MOIS = 3;

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
     * Recalcule les mois bonus du parrain selon le nombre de filleuls actifs.
     * Règle : 1 mois offert par tranche de 3 filleuls actifs.
     */
    @Transactional
    public void processActivation(VendorUser newVendor) {
        if (newVendor.getReferredById() == null) return;
        Optional<VendorUser> parrainOpt = vendorRepo.findById(newVendor.getReferredById());
        parrainOpt.ifPresent(parrain -> {
            // Compter les filleuls actifs (plan payant)
            long activeFilleuls = countActiveFilleuls(parrain);
            // Calculer les mois bonus : 1 mois par tranche de FILLEULS_PAR_MOIS
            int bonusMois = (int) (activeFilleuls / FILLEULS_PAR_MOIS);
            parrain.setReferralBonusMonths(bonusMois);
            vendorRepo.save(parrain);
            log.info("🎁 Parrainage : vendeur#{} a {} filleuls actifs → {} mois bonus",
                    parrain.getId(), activeFilleuls, bonusMois);
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

    /** Compte les filleuls actifs (plan payant) d'un parrain */
    public long countActiveFilleuls(VendorUser parrain) {
        List<VendorUser> filleuls = vendorRepo.findByReferredById(parrain.getId());
        return filleuls.stream()
                .filter(f -> f.getPlan() != null && f.getPlan() != VendorPlan.GRATUIT
                        && f.isActive())
                .count();
    }

    /** Progression vers le prochain mois bonus */
    public long filleulsVersProchainMois(VendorUser parrain) {
        long active = countActiveFilleuls(parrain);
        return active % FILLEULS_PAR_MOIS;
    }

    /** Filleuls restants pour le prochain mois */
    public long filleulsRestants(VendorUser parrain) {
        return FILLEULS_PAR_MOIS - filleulsVersProchainMois(parrain);
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
