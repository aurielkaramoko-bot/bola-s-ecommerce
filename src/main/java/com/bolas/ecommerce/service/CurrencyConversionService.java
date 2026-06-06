package com.bolas.ecommerce.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Convertit un montant XOF vers la devise cible.
 * Utilise https://api.exchangerate-api.com/v4/latest/XOF (sans clé API).
 * Résultat mis en cache pendant 1 heure.
 */
@Service
public class CurrencyConversionService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyConversionService.class);
    private static final String API_URL = "https://api.exchangerate-api.com/v4/latest/XOF";
    private static final long CACHE_TTL_SECONDS = 3600;

    private final RestTemplate restTemplate = new RestTemplate();

    // cache : devise → taux depuis XOF
    private final Map<String, Double> rateCache = new ConcurrentHashMap<>();
    private volatile Instant cacheExpiry = Instant.EPOCH;

    /**
     * Convertit {@code amountXof} FCFA vers {@code targetCurrency}.
     * Retourne null si la conversion échoue ou si la devise == XOF/XAF.
     */
    public ConversionResult convert(long amountXof, String targetCurrency) {
        if (targetCurrency == null) return null;
        String target = targetCurrency.toUpperCase();
        // XOF et XAF sont proches (1:1 légalement), pas de conversion nécessaire
        if ("XOF".equals(target) || "XAF".equals(target)) return null;

        ensureCacheLoaded();
        Double rate = rateCache.get(target);
        if (rate == null || rate <= 0) return null;

        long converted = Math.round(amountXof * rate);
        return new ConversionResult(converted, target);
    }

    private void ensureCacheLoaded() {
        if (Instant.now().isBefore(cacheExpiry) && !rateCache.isEmpty()) return;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(API_URL, Map.class);
            if (response != null && response.get("rates") instanceof Map<?, ?> rates) {
                rateCache.clear();
                rates.forEach((k, v) -> {
                    if (v instanceof Number n) rateCache.put(k.toString(), n.doubleValue());
                });
                cacheExpiry = Instant.now().plusSeconds(CACHE_TTL_SECONDS);
                log.info("Taux de change rechargés ({} devises)", rateCache.size());
            }
        } catch (Exception e) {
            log.warn("Impossible de charger les taux de change: {}", e.getMessage());
        }
    }

    public record ConversionResult(long amount, String currency) {}
}
