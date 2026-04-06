package com.bolas.ecommerce.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Journalisation des actions sensibles de l'admin.
 * Les logs sont écrits via SLF4J — configurables dans application.properties.
 */
@Service
public class AuditLogService {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    // ---- Produits ----

    public void productCreated(Long id, String name) {
        AUDIT.info("[PRODUIT][CREATION] id={} nom=\"{}\" par={}", id, name, who());
    }

    public void productUpdated(Long id, String name) {
        AUDIT.info("[PRODUIT][MODIFICATION] id={} nom=\"{}\" par={}", id, name, who());
    }

    public void productDeleted(Long id, String name) {
        AUDIT.warn("[PRODUIT][SUPPRESSION] id={} nom=\"{}\" par={}", id, name, who());
    }

    // ---- Catégories ----

    public void categoryCreated(Long id, String name) {
        AUDIT.info("[CATEGORIE][CREATION] id={} nom=\"{}\" par={}", id, name, who());
    }

    public void categoryUpdated(Long id, String name) {
        AUDIT.info("[CATEGORIE][MODIFICATION] id={} nom=\"{}\" par={}", id, name, who());
    }

    public void categoryDeleted(Long id, String name) {
        AUDIT.warn("[CATEGORIE][SUPPRESSION] id={} nom=\"{}\" par={}", id, name, who());
    }

    // ---- Commandes ----

    public void orderStatusChanged(Long id, String tracking, String newStatus) {
        AUDIT.info("[COMMANDE][STATUT] id={} tracking={} nouveau_statut={} par={}",
                id, tracking, newStatus, who());
    }

    public void orderDeleted(Long id, String tracking) {
        AUDIT.warn("[COMMANDE][SUPPRESSION] id={} tracking={} par={}", id, tracking, who());
    }

    // ---- Livraison GPS ----

    public void courierPositionUpdated(Long orderId, String tracking, double lat, double lng) {
        AUDIT.info("[LIVRAISON][GPS] orderId={} tracking={} lat={} lng={} par={}",
                orderId, tracking, lat, lng, who());
    }

    // ---- Connexion ----

    public void loginSuccess(String username, String ip) {
        AUDIT.info("[AUTH][CONNEXION_OK] user={} ip={}", username, ip);
    }

    public void loginFailure(String username, String ip) {
        AUDIT.warn("[AUTH][ECHEC_CONNEXION] user={} ip={}", username, ip);
    }

    // ---- Utilitaire ----

    private String who() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "anonyme";
    }
}
