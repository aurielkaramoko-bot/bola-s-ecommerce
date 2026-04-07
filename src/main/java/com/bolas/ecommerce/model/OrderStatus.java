package com.bolas.ecommerce.model;

public enum OrderStatus {
    /** Commande reçue — en attente de validation admin */
    PENDING,
    /** Admin a validé — tracking généré, vendeur notifié */
    CONFIRMED,
    /** Vendeur a préparé la commande — en attente de livraison */
    READY,
    /** En cours de livraison */
    IN_DELIVERY,
    /** Livrée */
    DELIVERED,
    /** Annulée */
    CANCELLED
}
