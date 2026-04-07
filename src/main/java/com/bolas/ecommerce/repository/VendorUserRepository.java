package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.VendorUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VendorUserRepository extends JpaRepository<VendorUser, Long> {
    Optional<VendorUser> findByUsername(String username);
}
