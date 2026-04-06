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

    public String productOrderUrl(Product p) {
        String option = p.isDeliveryAvailable()
                ? "Livraison à domicile ou retrait en boutique"
                : "Retrait en boutique";
        String msg = "Bonjour Bola's, je veux commander :\n"
                + "Produit : " + p.getName() + "\n"
                + "Prix : " + p.getEffectivePriceCfa() + " CFA\n"
                + "Option : " + option;
        return "https://wa.me/" + whatsappNumber + "?text="
                + URLEncoder.encode(msg, StandardCharsets.UTF_8);
    }
}
