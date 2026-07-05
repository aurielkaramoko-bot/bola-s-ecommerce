package com.bolas.ecommerce.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job planifié pour recalculer les TrustScores de tous les vendeurs actifs.
 * S'exécute toutes les 6 heures.
 */
@Component
public class TrustScoreScheduler {

    private static final Logger log = LoggerFactory.getLogger(TrustScoreScheduler.class);

    private final VendorTrustScoreService trustScoreService;

    public TrustScoreScheduler(VendorTrustScoreService trustScoreService) {
        this.trustScoreService = trustScoreService;
    }

    /**
     * Recalcul automatique toutes les 6 heures.
     * Cron : 0 0 0,6,12,18 * * * (à minuit, 6h, 12h, 18h)
     */
    @Scheduled(cron = "0 0 0,6,12,18 * * *")
    public void recalculateAllScores() {
        log.info("🛡️ [TrustScore] Début du recalcul planifié...");
        try {
            int count = trustScoreService.recalculateAll();
            log.info("🛡️ [TrustScore] Recalcul terminé : {} vendeurs traités.", count);
        } catch (Exception e) {
            log.error("🛡️ [TrustScore] Erreur lors du recalcul planifié", e);
        }
    }
}
