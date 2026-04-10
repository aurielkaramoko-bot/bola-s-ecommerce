package com.bolas.ecommerce.model;

public enum CourierApplicationStatus {
    PENDING,            // En attente de validation admin
    APPROVED,           // Approuvé
    REJECTED,           // Rejeté
    SUSPENDED_SOFT,     // Suspendu doux (peut revenir)
    SUSPENDED_TOTAL,    // Suspendu total (bloqué)
    DELETED             // Supprimé
}
