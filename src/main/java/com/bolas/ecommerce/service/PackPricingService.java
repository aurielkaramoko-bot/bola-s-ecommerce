package com.bolas.ecommerce.service;

import com.bolas.ecommerce.model.AppSetting;
import com.bolas.ecommerce.repository.AppSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class PackPricingService {

    private static final Logger log = LoggerFactory.getLogger(PackPricingService.class);

    private static final String KEY_GRATUIT   = "pack.price.GRATUIT";
    private static final String KEY_PRO_LOCAL = "pack.price.PRO_LOCAL";
    private static final String KEY_PRO       = "pack.price.PRO";
    private static final String KEY_PREMIUM   = "pack.price.PREMIUM";

    // Valeurs par défaut depuis application.properties (fallback si rien en base)
    @Value("${bolas.pack.price.gratuit:0}")
    private int defaultGratuit;

    @Value("${bolas.pack.price.pro-local:3000}")
    private int defaultProLocal;

    @Value("${bolas.pack.price.pro:3000}")
    private int defaultPro;

    @Value("${bolas.pack.price.premium:10000}")
    private int defaultPremium;

    private final AppSettingRepository settingRepository;

    public PackPricingService(AppSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    private int get(String key, int defaultValue) {
        return settingRepository.findById(key)
                .map(s -> {
                    try { return Integer.parseInt(s.getValue()); }
                    catch (NumberFormatException e) { return defaultValue; }
                })
                .orElse(defaultValue);
    }

    public int getGratuitPrice()   { return get(KEY_GRATUIT,   defaultGratuit); }
    public int getProLocalPrice()  { return get(KEY_PRO_LOCAL, defaultProLocal); }
    public int getProPrice()       { return get(KEY_PRO,       defaultPro); }
    public int getPremiumPrice()   { return get(KEY_PREMIUM,   defaultPremium); }

    public int getPriceForPlan(String planName) {
        return switch (planName != null ? planName.toUpperCase() : "") {
            case "GRATUIT"   -> getGratuitPrice();
            case "PRO_LOCAL" -> getProLocalPrice();
            case "PRO"       -> getProPrice();
            case "PREMIUM"   -> getPremiumPrice();
            default          -> 0;
        };
    }

    public Map<String, Integer> getAllPrices() {
        return Map.of(
            "GRATUIT",   getGratuitPrice(),
            "PRO_LOCAL", getProLocalPrice(),
            "PRO",       getProPrice(),
            "PREMIUM",   getPremiumPrice()
        );
    }

    @Transactional
    public void updatePrice(String planName, int price) {
        String key = switch (planName.toUpperCase()) {
            case "GRATUIT"   -> KEY_GRATUIT;
            case "PRO_LOCAL" -> KEY_PRO_LOCAL;
            case "PRO"       -> KEY_PRO;
            case "PREMIUM"   -> KEY_PREMIUM;
            default -> null;
        };
        if (key == null) return;
        settingRepository.save(new AppSetting(key, String.valueOf(price)));
        log.info("✅ Prix {} mis à jour : {} FCFA", planName, price);
    }
}
