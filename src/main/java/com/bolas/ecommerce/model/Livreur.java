package com.bolas.ecommerce.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * Compte livreur dédié — identifié par téléphone + PIN.
 * Lié aux commandes via assignedCourierPhone.
 */
@Entity
@Table(name = "livreurs")
public class Livreur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String name;

    @NotBlank @Size(max = 40)
    @Column(nullable = false, unique = true, length = 40)
    private String phone;

    /** PIN hashé (BCrypt) — 4 chiffres minimum */
    @NotBlank
    @Column(nullable = false, length = 200)
    private String pinHash;

    /** Immatriculation du véhicule (ex: TG-1234-AB) */
    @Size(max = 30)
    @Column(name = "vehicle_plate", length = 30)
    private String vehiclePlate;

    /** Type de véhicule : MOTO, VOITURE, TRICYCLE, VELO */
    @Size(max = 20)
    @Column(name = "vehicle_type", length = 20)
    private String vehicleType;

    /** Photo du véhicule (Cloudinary URL) */
    @Size(max = 2000)
    @Column(name = "vehicle_photo_url", length = 2000)
    private String vehiclePhotoUrl;

    /** Zone de livraison couverte */
    @Size(max = 300)
    @Column(name = "delivery_zone", length = 300)
    private String deliveryZone;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPinHash() { return pinHash; }
    public void setPinHash(String pinHash) { this.pinHash = pinHash; }
    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public String getVehiclePhotoUrl() { return vehiclePhotoUrl; }
    public void setVehiclePhotoUrl(String vehiclePhotoUrl) { this.vehiclePhotoUrl = vehiclePhotoUrl; }
    public String getDeliveryZone() { return deliveryZone; }
    public void setDeliveryZone(String deliveryZone) { this.deliveryZone = deliveryZone; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
}
