package com.bolas.ecommerce.dto;

public record TrackingPublicResponse(
        String trackingNumber,
        String status,
        Double courierLatitude,
        Double courierLongitude,
        Double clientLatitude,
        Double clientLongitude,
        String courierPhone,
        String courierVehiclePlate,
        String courierPhotoUrl
) {
}
