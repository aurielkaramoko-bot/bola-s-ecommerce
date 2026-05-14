package com.bolas.ecommerce.service;

import com.bolas.ecommerce.model.CustomerLoyaltyAccount;
import com.bolas.ecommerce.repository.CustomerLoyaltyAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion des points de fidélité client BOLA (clé = téléphone).
 * Appelé depuis OrderFlowService quand une commande passe en DELIVERED.
 * 1 point = 100 CFA dépensés.
 */
@Service
public class CustomerLoyaltyService {

    private static final Logger log = LoggerFactory.getLogger(CustomerLoyaltyService.class);
    private static final int CFA_PER_POINT = 100;

    private final CustomerLoyaltyAccountRepository repo;

    public CustomerLoyaltyService(CustomerLoyaltyAccountRepository repo) {
        this.repo = repo;
    }

    /** Crédite les points après une commande livrée (clé = téléphone client) */
    @Transactional
    public int crediterPoints(String customerPhone, long montantCfa) {
        if (customerPhone == null || customerPhone.isBlank() || montantCfa <= 0) return 0;
        int pts = (int) (montantCfa / CFA_PER_POINT);
        if (pts <= 0) return 0;
        try {
            if (repo.findByCustomerPhone(customerPhone).isPresent()) {
                repo.addPoints(customerPhone, pts);
            } else {
                CustomerLoyaltyAccount acc = new CustomerLoyaltyAccount();
                acc.setCustomerPhone(customerPhone);
                acc.setPoints(pts);
                acc.setTotalOrders(1);
                repo.save(acc);
            }
            log.info("💰 Fidélité : +{} pts téléphone {}", pts, customerPhone);
        } catch (Exception e) {
            log.warn("⚠️ Fidélité non créditée {}: {}", customerPhone, e.getMessage());
        }
        return pts;
    }

    /** Calcule les points que le client gagnerait pour un montant donné */
    public int calculerPoints(long montantCfa) {
        return (int) (montantCfa / CFA_PER_POINT);
    }

    /** Récupère ou crée le compte fidélité par téléphone */
    @Transactional
    public CustomerLoyaltyAccount getOrCreate(String customerPhone) {
        return repo.findByCustomerPhone(customerPhone).orElseGet(() -> {
            CustomerLoyaltyAccount acc = new CustomerLoyaltyAccount();
            acc.setCustomerPhone(customerPhone);
            return repo.save(acc);
        });
    }
}
