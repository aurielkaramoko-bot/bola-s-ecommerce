package com.bolas.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO pour l'enregistrement d'un token FCM.
 */
public class FcmTokenDto {

    @NotBlank(message = "token FCM manquant")
    @Size(max = 512, message = "token FCM trop long")
    private String token;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
