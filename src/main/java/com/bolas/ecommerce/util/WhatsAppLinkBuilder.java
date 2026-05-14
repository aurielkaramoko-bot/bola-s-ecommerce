package com.bolas.ecommerce.util;

import com.bolas.ecommerce.model.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component("whatsappLinkBuilder")
public class WhatsAppLinkBuilder {

    @Value("${whatsapp.number}")
    private String whatsappNumber;

    /**
     * Lien WhatsApp pour commander un produit depuis la fiche produit.
     * Inclut l'adresse texte saisie par le client + instruction localisation GPS.
     *
     * @param p               produit commandé
     * @param deliveryAddress adresse texte libre saisie par le client (peut être null/vide)
     */
    public String productOrderUrl(Product p, String deliveryAddress) {
        String option = p.isDeliveryAvailable()
                ? "Livraison à domicile ou retrait en boutique"
                : "Retrait en boutique";

        StringBuilder msg = new StringBuilder();
        msg.append("Bonjour Bola's, je veux commander :\n");
        msg.append("Produit : ").append(p.getName()).append("\n");
        msg.append("Prix : ").append(p.getEffectivePriceCfa()).append(" CFA\n");
        msg.append("Option : ").append(option).append("\n");

        if (deliveryAddress != null && !deliveryAddress.isBlank()) {
            msg.append("📍 Adresse de livraison : ").append(deliveryAddress.trim()).append("\n");
        }

        msg.append("\n")
           .append("➡️ Après cet envoi, appuyez sur l'icône 📎 (trombone) ")
           .append("puis Localisation › Envoyer ma position actuelle ")
           .append("pour que le livreur vous trouve facilement.");

        return "https://wa.me/" + whatsappNumber + "?text="
                + URLEncoder.encode(msg.toString(), StandardCharsets.UTF_8);
    }

    /**
     * Rétrocompatibilité — appel sans adresse (anciens appels existants).
     */
    public String productOrderUrl(Product p) {
        return productOrderUrl(p, null);
    }
}
