package com.bolas.ecommerce.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CourierUpdateDto {

    @NotNull
    private Long orderId;

    @NotNull
    @DecimalMin(value = "-90.0",  message = "Latitude invalide (min -90)")
    @DecimalMax(value = "90.0",   message = "Latitude invalide (max 90)")
    private Double courierLatitude;

    @NotNull
    @DecimalMin(value = "-180.0", message = "Longitude invalide (min -180)")
    @DecimalMax(value = "180.0",  message = "Longitude invalide (max 180)")
    private Double courierLongitude;

    @Size(max = 40)
    @Pattern(regexp = "^[+\\d\\s\\-().]*$", message = "Numéro de téléphone invalide")
    private String courierPhone;

    @Size(max = 32)
    @Pattern(regexp = "^[A-Za-z0-9\\-\\s]*$", message = "Plaque d'immatriculation invalide")
    private String courierVehiclePlate;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Double getCourierLatitude() {
        return courierLatitude;
    }

    public void setCourierLatitude(Double courierLatitude) {
        this.courierLatitude = courierLatitude;
    }

    public Double getCourierLongitude() {
        return courierLongitude;
    }

    public void setCourierLongitude(Double courierLongitude) {
        this.courierLongitude = courierLongitude;
    }

    public String getCourierPhone() {
        return courierPhone;
    }

    public void setCourierPhone(String courierPhone) {
        this.courierPhone = courierPhone;
    }

    public String getCourierVehiclePlate() {
        return courierVehiclePlate;
    }

    public void setCourierVehiclePlate(String courierVehiclePlate) {
        this.courierVehiclePlate = courierVehiclePlate;
    }
}
