package com.bolas.ecommerce.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PackPricingService {

    private static final Logger log = LoggerFactory.getLogger(PackPricingService.class);

    @Value("${bolas.pack.price.gratuit:0}")
    private int gratuitPrice;

    @Value("${bolas.pack.price.pro-local:3000}")
    private int proLocalPrice;

    @Value("${bolas.pack.price.pro:3000}")
    private int proPrice;

    @Value("${bolas.pack.price.premium:10000}")
    private int premiumPrice;

    /**
     * Obtient le prix d'un plan spécifique
     */
    public int getPriceForPlan(String planName) {
        return switch(planName != null ? planName.toUpperCase() : "") {
            case "GRATUIT" -> gratuitPrice;
            case "PRO_LOCAL" -> proLocalPrice;
            case "PRO" -> proPrice;
            case "PREMIUM" -> premiumPrice;
            default -> 0;
        };
    }

    /**
     * Obtient tous les prix sous forme de map
     */
    public Map<String, Integer> getAllPrices() {
        Map<String, Integer> prices = new HashMap<>();
        prices.put("GRATUIT", gratuitPrice);
        prices.put("PRO_LOCAL", proLocalPrice);
        prices.put("PRO", proPrice);
        prices.put("PREMIUM", premiumPrice);
        return prices;
    }

    /**
     * Met à jour les prix dynamiquement
     */
    public void updatePrice(String planName, int price) {
        switch(planName.toUpperCase()) {
            case "GRATUIT":
                gratuitPrice = price;
                log.info("✅ Prix GRATUIT mis à jour: {} FCFA", price);
                break;
            case "PRO_LOCAL":
                proLocalPrice = price;
                log.info("✅ Prix PRO_LOCAL mis à jour: {} FCFA", price);
                break;
            case "PRO":
                proPrice = price;
                log.info("✅ Prix PRO mis à jour: {} FCFA", price);
                break;
            case "PREMIUM":
                premiumPrice = price;
                log.info("✅ Prix PREMIUM mis à jour: {} FCFA", price);
                break;
        }
    }

    // Getters pour accès direct
    public int getGratuitPrice() {
        return gratuitPrice;
    }

    public int getProLocalPrice() {
        return proLocalPrice;
    }

    public int getProPrice() {
        return proPrice;
    }

    public int getPremiumPrice() {
        return premiumPrice;
    }
}
