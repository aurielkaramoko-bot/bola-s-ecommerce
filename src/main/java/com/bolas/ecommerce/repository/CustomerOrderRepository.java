package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    Optional<CustomerOrder> findByTrackingNumber(String trackingNumber);

    Optional<CustomerOrder> findByCourierToken(String courierToken);

    List<CustomerOrder> findTop10ByOrderByCreatedAtDesc();

    List<CustomerOrder> findAllByOrderByCreatedAtDesc();
}
