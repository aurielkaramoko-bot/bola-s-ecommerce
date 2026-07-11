package com.bolas.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO pour la création d'un commentaire produit.
 */
public class ProductCommentDto {

    @NotBlank(message = "Le texte du commentaire est obligatoire")
    @Size(min = 1, max = 1000, message = "Le commentaire doit faire entre 1 et 1000 caractères")
    private String text;

    private Long parentId;

    @Pattern(regexp = "https://.*", message = "L'URL de la photo doit commencer par https://")
    @Size(max = 500)
    private String photoUrl;

    public String getText()        { return text; }
    public void setText(String t)  { this.text = t; }

    public Long getParentId()           { return parentId; }
    public void setParentId(Long id)    { this.parentId = id; }

    public String getPhotoUrl()         { return photoUrl; }
    public void setPhotoUrl(String url) { this.photoUrl = url; }
}
