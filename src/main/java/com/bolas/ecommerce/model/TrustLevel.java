package com.bolas.ecommerce.model;

/**
 * Niveaux de confiance vendeur, dérivés du TrustScore (0-100).
 * Chaque niveau a un label français, un emoji et une couleur CSS.
 *
 * Seuils :
 *  0-20  → NEW        (Nouveau)
 *  21-40 → BASIC      (Basique)
 *  41-60 → TRUSTED    (De confiance)
 *  61-80 → VERIFIED   (Vérifié)
 *  81-100→ STAR_SELLER(Vendeur étoile)
 */
public enum TrustLevel {

    NEW("Nouveau", "🆕", "#6c757d", 0, 20),
    BASIC("Basique", "🔵", "#0d6efd", 21, 40),
    TRUSTED("De confiance", "🟢", "#198754", 41, 60),
    VERIFIED("Vérifié", "✅", "#0dcaf0", 61, 80),
    STAR_SELLER("Vendeur étoile", "⭐", "#ffc107", 81, 100);

    private final String label;
    private final String emoji;
    private final String cssColor;
    private final int minScore;
    private final int maxScore;

    TrustLevel(String label, String emoji, String cssColor, int minScore, int maxScore) {
        this.label = label;
        this.emoji = emoji;
        this.cssColor = cssColor;
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public String getLabel() { return label; }
    public String getEmoji() { return emoji; }
    public String getCssColor() { return cssColor; }
    public int getMinScore() { return minScore; }
    public int getMaxScore() { return maxScore; }

    /** Retourne le badge affiché : emoji + label (ex: "⭐ Vendeur étoile") */
    public String getBadgeText() {
        return emoji + " " + label;
    }

    /** Détermine le TrustLevel à partir d'un score 0-100 */
    public static TrustLevel fromScore(int score) {
        if (score >= 81) return STAR_SELLER;
        if (score >= 61) return VERIFIED;
        if (score >= 41) return TRUSTED;
        if (score >= 21) return BASIC;
        return NEW;
    }
}
