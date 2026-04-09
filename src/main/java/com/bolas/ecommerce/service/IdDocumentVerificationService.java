package com.bolas.ecommerce.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Vérifie qu'une image uploadée contient bien un document d'identité
 * (CNI, passeport, permis) via Google Cloud Vision API (TEXT_DETECTION).
 *
 * Mots-clés recherchés dans le texte extrait :
 *  - Mots génériques : REPUBLIQUE, CARTE, NATIONALE, IDENTITE, PASSEPORT,
 *                      PASSPORT, PERMIS, CONDUIRE, NATIONAL, IDENTITY, CARD
 *  - Mots spécifiques Togo/Afrique de l'Ouest : TOGOLAISE, TOGOLAIS, BENIN,
 *                      GHANA, NIGERIA, SENEGAL, COTE D'IVOIRE, BURKINA
 *
 * Si la clé API n'est pas configurée → retourne null (non bloquant, admin voit manuellement).
 */
@Service
public class IdDocumentVerificationService {

    private static final Logger log = LoggerFactory.getLogger(IdDocumentVerificationService.class);

    // Mots-clés qui indiquent un document d'identité officiel
    private static final List<String> ID_KEYWORDS = List.of(
            "republique", "republic", "carte nationale", "national identity",
            "identity card", "carte d'identite", "identite nationale",
            "passeport", "passport", "permis de conduire", "driving licence",
            "driver license", "togolaise", "togolais", "benin", "ghana",
            "nigeria", "senegal", "burkina", "cote d'ivoire", "ivoire",
            "nom", "prenom", "date de naissance", "date of birth",
            "nationalite", "nationality", "expire", "expiry", "delivre",
            "issued", "numéro", "numero", "n°"
    );

    private static final String VISION_URL =
            "https://vision.googleapis.com/v1/images:annotate?key=";

    private final String apiKey;
    private final HttpClient httpClient;

    public IdDocumentVerificationService(
            @Value("${google.vision.api.key:}") String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * @return true  → document d'identité détecté
     *         false → image non reconnue comme document d'identité
     *         null  → Vision API non configurée (clé absente), vérification ignorée
     */
    public Boolean verify(MultipartFile file) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Google Vision API key non configurée — vérification CNI ignorée.");
            return null;
        }
        try {
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            String body = buildRequestBody(base64);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(VISION_URL + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Vision API error {}: {}", response.statusCode(), response.body());
                return null; // non bloquant si l'API est down
            }

            String responseText = response.body().toLowerCase(Locale.ROOT);
            return containsIdKeyword(responseText);

        } catch (IOException | InterruptedException e) {
            log.error("Erreur appel Vision API", e);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private boolean containsIdKeyword(String visionResponseLower) {
        for (String keyword : ID_KEYWORDS) {
            if (visionResponseLower.contains(keyword)) {
                log.debug("Mot-clé CNI trouvé : '{}'", keyword);
                return true;
            }
        }
        return false;
    }

    private String buildRequestBody(String base64Image) {
        return """
                {
                  "requests": [{
                    "image": { "content": "%s" },
                    "features": [
                      { "type": "TEXT_DETECTION", "maxResults": 1 },
                      { "type": "DOCUMENT_TEXT_DETECTION", "maxResults": 1 }
                    ]
                  }]
                }
                """.formatted(base64Image);
    }
}
