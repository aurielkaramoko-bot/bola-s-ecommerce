package com.bolas.ecommerce.model;

public enum OrderStatus {
    /** Commande reçue — en attente de traitement vendeur */
    PENDING,
    /** Vendeur a confirmé ET préparé — prête pour livraison */
    READY,
    /** En cours de livraison */
    IN_DELIVERY,
    /** Livrée */
    DELIVERED,
    /** Annulée */
    CANCELLED
}
