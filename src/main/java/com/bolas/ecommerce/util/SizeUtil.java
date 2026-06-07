package com.bolas.ecommerce.util;

import java.util.List;

/**
 * Mapping catégorie → options de taille disponibles.
 * La détection se base sur le nom de la catégorie (insensible à la casse).
 */
public final class SizeUtil {

    public static final List<String> SHOE_SIZES  = List.of("35","36","37","38","39","40","41","42","43","44","45","46");
    public static final List<String> CLOTH_SIZES = List.of("XS","S","M","L","XL","XXL","XXXL");

    /** Retourne le type de taille selon le nom de catégorie, ou null si aucune taille applicable */
    public static String sizeType(String categoryName) {
        if (categoryName == null) return null;
        String n = categoryName.toLowerCase();
        if (n.contains("chaussure") || n.contains("sneaker") || n.contains("basket")
                || n.contains("sandale") || n.contains("botte") || n.contains("escarpin")) {
            return "SHOES";
        }
        if (n.contains("mode") || n.contains("vêtement") || n.contains("vetement")
                || n.contains("robe") || n.contains("chemise") || n.contains("pantalon")
                || n.contains("pull") || n.contains("modeste") || n.contains("tenue")
                || n.contains("habillement") || n.contains("lingerie") || n.contains("maillot")) {
            return "CLOTHES";
        }
        return null;
    }

    /** Retourne la liste de tailles pour un type donné */
    public static List<String> sizesFor(String type) {
        if ("SHOES".equals(type)) return SHOE_SIZES;
        if ("CLOTHES".equals(type)) return CLOTH_SIZES;
        return List.of();
    }

    /** Parse une chaîne CSV de tailles → liste */
    public static List<String> parse(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return List.of(csv.split(",")).stream()
                .map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    /** Encode une liste de tailles → CSV */
    public static String encode(List<String> sizes) {
        if (sizes == null || sizes.isEmpty()) return null;
        return String.join(",", sizes);
    }

    private SizeUtil() {}
}
