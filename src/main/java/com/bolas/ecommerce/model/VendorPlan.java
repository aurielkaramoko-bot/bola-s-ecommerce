package com.bolas.ecommerce.model;

public enum VendorPlan {
    GRATUIT,    // 10 produits max, accès basique — gratuit
    PRO_LOCAL,  // Produits illimités, stats — 3 000 FCFA/mois (boutiques locales)
    PRO,        // Produits illimités, stats, mise en avant — 3 000 FCFA/mois (alias)
    PREMIUM     // Tout PRO + support prioritaire + badge vérifié — 10 000 FCFA/mois
}
