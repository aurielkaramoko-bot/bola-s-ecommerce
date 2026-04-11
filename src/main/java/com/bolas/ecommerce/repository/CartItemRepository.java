package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.CartItem;
import com.bolas.ecommerce.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCustomer(Customer customer);
    Optional<CartItem> findByCustomerAndProductId(Customer customer, Long productId);
    void deleteByCustomer(Customer customer);
}