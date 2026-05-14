package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.CustomerLoyaltyAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerLoyaltyAccountRepository extends JpaRepository<CustomerLoyaltyAccount, Long> {

    Optional<CustomerLoyaltyAccount> findByCustomerPhone(String customerPhone);

    @Modifying
    @Query("UPDATE CustomerLoyaltyAccount a SET a.points = a.points + :pts, " +
           "a.totalOrders = a.totalOrders + 1, a.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE a.customerPhone = :phone")
    void addPoints(@Param("phone") String customerPhone, @Param("pts") int points);
}
