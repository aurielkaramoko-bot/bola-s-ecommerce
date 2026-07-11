package com.bolas.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * DTO pour l'enregistrement d'une interaction client-produit.
 */
public class InteractionDto {

    @NotNull(message = "customerId est obligatoire")
    @Positive
    private Long customerId;

    @NotNull(message = "productId est obligatoire")
    @Positive
    private Long productId;

    @NotBlank(message = "type est obligatoire")
    @Pattern(regexp = "VIEW|ADD_TO_CART|PURCHASE|REVIEW",
             message = "type doit être VIEW, ADD_TO_CART, PURCHASE ou REVIEW")
    private String type;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
