package com.bolas.ecommerce.api;

import com.bolas.ecommerce.dto.TrackingPublicResponse;
import com.bolas.ecommerce.repository.CustomerOrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TrackingApiController {

    private final CustomerOrderRepository customerOrderRepository;

    public TrackingApiController(CustomerOrderRepository customerOrderRepository) {
        this.customerOrderRepository = customerOrderRepository;
    }

    @GetMapping("/api/public/tracking/{code}")
    public ResponseEntity<TrackingPublicResponse> tracking(@PathVariable String code) {
        return customerOrderRepository.findByTrackingNumber(code)
                .map(o -> new TrackingPublicResponse(
                        o.getTrackingNumber(),
                        o.getStatus().name(),
                        o.getCourierLatitude(),
                        o.getCourierLongitude(),
                        o.getClientLatitude(),
                        o.getClientLongitude(),
                        o.getCourierPhone(),
                        o.getCourierVehiclePlate(),
                        o.getCourierPhotoUrl()
                ))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
