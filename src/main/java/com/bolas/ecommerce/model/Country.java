package com.bolas.ecommerce.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Pays dans lesquels BOLA opère.
 * Gérable depuis l'admin — le compteur homepage est dynamique.
 */
@Entity
@Table(name = "countries")
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Code ISO 2 lettres : TG, CI, BJ... */
    @NotBlank @Size(max = 4)
    @Column(nullable = false, unique = true, length = 4)
    private String code;

    /** Nom affiché : Togo, Côte d'Ivoire... */
    @NotBlank @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    /** Emoji drapeau : 🇹🇬 */
    @Size(max = 10)
    @Column(length = 10)
    private String flag;

    @Column(nullable = false)
    private boolean active = true;

    /**
     * Taxe douanière appliquée aux commandes vers ce pays (en %).
     * Ex : 15 = 15%. 0 = pas de taxe (pays local).
     */
    @Column(nullable = true)
    private Integer customsTaxPercent = 0;

    public Long getId() { return id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFlag() { return flag; }
    public void setFlag(String flag) { this.flag = flag; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getCustomsTaxPercent() { return customsTaxPercent != null ? customsTaxPercent : 0; }
    public void setCustomsTaxPercent(int customsTaxPercent) { this.customsTaxPercent = customsTaxPercent; }
}
