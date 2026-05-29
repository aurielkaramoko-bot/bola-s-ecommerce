package com.bolas.ecommerce.model;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    READY,
    IN_DELIVERY,
    DELIVERED,
    CANCELLED;

    public String getLabel() {
        return switch (this) {
            case PENDING    -> "En attente";
            case CONFIRMED  -> "Confirmée";
            case READY      -> "Prête";
            case IN_DELIVERY -> "En livraison";
            case DELIVERED  -> "Livrée";
            case CANCELLED  -> "Annulée";
        };
    }
}
