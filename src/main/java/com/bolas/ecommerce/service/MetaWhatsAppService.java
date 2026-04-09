package com.bolas.ecommerce.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Envoie des messages WhatsApp automatiquement via Meta Cloud API.
 *
 * Configuration requise dans application.properties (ou variables d'env Render) :
 *   meta.whatsapp.token       = ton Bearer token (EAAxxxxx...)
 *   meta.whatsapp.phone-id    = ton Phone Number ID (ex: 123456789012345)
 *
 * Doc : https://developers.facebook.com/docs/whatsapp/cloud-api/messages
 *
 * Si le token n'est pas configuré, les envois sont ignorés silencieusement.
 */
@Service
public class MetaWhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(MetaWhatsAppService.class);
    private static final String API_URL = "https://graph.facebook.com/v19.0/%s/messages";

    private final String token;
    private final String phoneId;
    private final HttpClient http;

    public MetaWhatsAppService(
            @Value("${meta.whatsapp.token:}") String token,
            @Value("${meta.whatsapp.phone-id:}") String phoneId) {
        this.token   = token;
        this.phoneId = phoneId;
        this.http    = HttpClient.newHttpClient();
    }

    public boolean isConfigured() {
        return token != null && !token.isBlank()
            && phoneId != null && !phoneId.isBlank();
    }

    /**
     * Envoie un message texte libre à un numéro WhatsApp.
     *
     * @param toPhone numéro international sans + ni espaces (ex: 22870099525)
     * @param text    texte du message (max ~4096 chars)
     */
    public void sendText(String toPhone, String text) {
        if (!isConfigured()) {
            log.debug("Meta WhatsApp non configuré — message ignoré vers {}", toPhone);
            return;
        }
        String cleaned = toPhone.replaceAll("[^0-9]", "");
        String body = """
                {
                  "messaging_product": "whatsapp",
                  "to": "%s",
                  "type": "text",
                  "text": { "body": %s }
                }
                """.formatted(cleaned, jsonString(text));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL.formatted(phoneId)))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                log.info("WhatsApp envoyé à {} ✓", cleaned);
            } else {
                log.warn("WhatsApp API erreur {} → {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Erreur envoi WhatsApp à {}: {}", cleaned, e.getMessage());
        }
    }

    /** Échappe une chaîne pour l'inclure dans un JSON string */
    private String jsonString(String s) {
        if (s == null) return "\"\"";
        String escaped = s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }
}
