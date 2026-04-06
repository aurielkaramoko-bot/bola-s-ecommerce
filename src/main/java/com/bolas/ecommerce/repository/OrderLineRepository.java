package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.OrderLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {

    long countByProduct_Id(Long productId);

    void deleteByProduct_Id(Long productId);
}
