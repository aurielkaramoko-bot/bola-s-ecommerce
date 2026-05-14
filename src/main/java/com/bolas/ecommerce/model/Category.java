package com.bolas.ecommerce.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Catégorie produit sur 3 niveaux :
 *  niveau 1 (parentId IS NULL)  = grande famille (ex: Mode Femme)
 *  niveau 2 (parentId → niv.1)  = sous-catégorie (ex: Robes)
 *  niveau 3 (parentId → niv.2)  = feuille cliquable (ex: Robes de soirée)
 */
@Entity
@Table(name = "categories",
       uniqueConstraints = @UniqueConstraint(columnNames = "slug"))
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    /** Slug URL-friendly unique (ex: mode-femme, robes-de-soiree) */
    @Size(max = 150)
    @Column(length = 150)
    private String slug;

    /** ID de la catégorie parente (null = catégorie racine niveau 1) */
    @Column(name = "parent_id")
    private Long parentId;

    /** Emoji d'affichage pour la grande famille (ex: 👗) */
    @Size(max = 10)
    @Column(length = 10)
    private String emoji;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    @Size(max = 2000)
    @Column(length = 2000)
    private String imageUrl;

    /** Catégorie active (l'admin peut désactiver sans supprimer) */
    @Column(nullable = false)
    private boolean active = true;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** Retourne true si c'est une catégorie racine (niveau 1) */
    public boolean isRoot() { return parentId == null; }

    /** Préfixe emoji + nom pour l'affichage */
    public String getDisplayName() {
        return (emoji != null && !emoji.isBlank()) ? emoji + " " + name : name;
    }

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

