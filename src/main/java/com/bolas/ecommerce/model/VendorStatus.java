package com.bolas.ecommerce.model;

public enum VendorStatus {
    PENDING,    // Demande soumise, en attente de validation admin
    ACTIVE,     // Vendeur approuvé et actif
    SUSPENDED   // Compte suspendu par l'admin
}
