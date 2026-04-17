package com.bolas.ecommerce.model;

public enum VendorPlan {
    GRATUIT,    // 10 produits max, accès basique, commandes gérées par admin — 0 FCFA
    PRO_LOCAL,  // Produits illimités, gère ses commandes, chat, stats basiques — 5 000 FCFA/mois
    PRO,        // Alias de PRO_LOCAL (rétro-compatible) — 5 000 FCFA/mois
    PREMIUM     // Tout PRO + badge vérifié, homepage, stats avancées, GPS — 10 000 FCFA/mois
}
