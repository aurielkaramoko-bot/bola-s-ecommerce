package com.bolas.ecommerce.api;

import com.bolas.ecommerce.model.Product;
import com.bolas.ecommerce.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Recherche par photo — analyse les couleurs dominantes de l'image uploadée
 * et lance une recherche textuelle sur les produits.
 * Pas d'IA externe requise : analyse côté serveur avec java.awt.
 */
@RestController
@RequestMapping("/api/search")
public class ImageSearchApiController {

    private static final Logger log = LoggerFactory.getLogger(ImageSearchApiController.class);
    private final ProductRepository productRepository;

    public ImageSearchApiController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @PostMapping("/image")
    public ResponseEntity<Map<String, Object>> searchByImage(
            @RequestParam("image") MultipartFile image) {

        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Image manquante"));
        }

        try {
            // Analyser les couleurs dominantes
            BufferedImage img = ImageIO.read(image.getInputStream());
            if (img == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Format d'image non supporté"));
            }

            String dominantColor = extractDominantColorName(img);
            String keywords = dominantColor;

            // Recherche dans les produits
            List<Product> results = productRepository.findAll().stream()
                    .filter(p -> p.isAvailable()
                            && (p.getVendor() == null || p.getVendor().isActive())
                            && matchesKeyword(p, keywords))
                    .limit(20)
                    .collect(Collectors.toList());

            // Si pas de résultats avec la couleur, chercher plus largement
            if (results.isEmpty()) {
                results = productRepository.findAllAvailablePremiumFirst().stream()
                        .limit(12)
                        .collect(Collectors.toList());
            }

            List<Map<String, Object>> productDtos = results.stream().map(p -> {
                Map<String, Object> dto = new LinkedHashMap<>();
                dto.put("id", p.getId());
                dto.put("name", p.getName());
                dto.put("price", p.getEffectivePriceCfa());
                dto.put("imageUrl", p.getImageUrl());
                dto.put("url", "/products/" + p.getId());
                return dto;
            }).toList();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("keywords", keywords);
            response.put("dominantColor", dominantColor);
            response.put("results", productDtos);
            response.put("count", productDtos.size());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Erreur analyse image : {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur lors de l'analyse de l'image"));
        }
    }

    /** Extrait le nom de la couleur dominante de l'image */
    private String extractDominantColorName(BufferedImage img) {
        // Échantillonner l'image (max 50x50 pour la perf)
        int sampleW = Math.min(img.getWidth(), 50);
        int sampleH = Math.min(img.getHeight(), 50);
        long r = 0, g = 0, b = 0;
        int count = 0;

        for (int x = 0; x < sampleW; x++) {
            for (int y = 0; y < sampleH; y++) {
                int rgb = img.getRGB(x * img.getWidth() / sampleW, y * img.getHeight() / sampleH);
                r += (rgb >> 16) & 0xFF;
                g += (rgb >> 8) & 0xFF;
                b += rgb & 0xFF;
                count++;
            }
        }

        if (count == 0) return "coloré";
        r /= count; g /= count; b /= count;

        // Mapper vers un nom de couleur
        return colorName((int) r, (int) g, (int) b);
    }

    private String colorName(int r, int g, int b) {
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        int brightness = (r + g + b) / 3;

        if (brightness < 50) return "noir";
        if (brightness > 200 && (max - min) < 30) return "blanc";
        if ((max - min) < 30) return "gris";

        if (r > g && r > b) {
            if (g > 100) return "orange";
            return "rouge";
        }
        if (g > r && g > b) return "vert";
        if (b > r && b > g) {
            if (r > 100) return "violet";
            return "bleu";
        }
        if (r > 150 && g > 100 && b < 80) return "marron";
        if (r > 200 && g > 150) return "jaune";
        return "coloré";
    }

    private boolean matchesKeyword(Product p, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String kw = keyword.toLowerCase();
        return (p.getName() != null && p.getName().toLowerCase().contains(kw))
                || (p.getDescription() != null && p.getDescription().toLowerCase().contains(kw))
                || (p.getCategory() != null && p.getCategory().getName().toLowerCase().contains(kw));
    }
}
