package com.bolas.ecommerce.model;

/**
 * Types d'interactions client-produit pour le moteur de recommandation IA.
 * Chaque type a un poids reflétant l'intensité de l'intérêt.
 */
public enum InteractionType {

    /** Le client a consulté la fiche produit */
    VIEW(1),

    /** Le client a ajouté le produit au panier */
    ADD_TO_CART(3),

    /** Le client a acheté le produit (commande confirmée) */
    PURCHASE(5),

    /** Le client a laissé un avis sur le produit */
    REVIEW(2);

    private final int weight;

    InteractionType(int weight) {
        this.weight = weight;
    }

    /** Poids de l'interaction pour le scoring IA */
    public int getWeight() {
        return weight;
    }
}
