package com.bolas.ecommerce.service;

import com.bolas.ecommerce.model.VendorPlan;
import org.springframework.stereotype.Service;

/**
 * Calcule la commission BOLA selon le plan du vendeur.
 *
 * GRATUIT   → 10% (pas de fidélité, on prend plus)
 * PRO_LOCAL → 7%
 * PRO       → 7%
 * PREMIUM   → 5% (récompense la fidélité)
 * null      → 0% (produit BOLA direct, pas de vendeur)
 */
@Service
public class CommissionService {

    public int rateFor(VendorPlan plan) {
        if (plan == null) return 0;
        return switch (plan) {
            case GRATUIT   -> 10;
            case PRO_LOCAL -> 7;
            case PRO       -> 7;
            case PREMIUM   -> 5;
        };
    }

    public long compute(long amountCfa, VendorPlan plan) {
        return amountCfa * rateFor(plan) / 100;
    }
}
