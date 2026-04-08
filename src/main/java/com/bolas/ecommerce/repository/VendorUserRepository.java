package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.VendorStatus;
import com.bolas.ecommerce.model.VendorUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorUserRepository extends JpaRepository<VendorUser, Long> {

    Optional<VendorUser> findByUsername(String username);

    Optional<VendorUser> findByEmail(String email);

    List<VendorUser> findByVendorStatus(VendorStatus vendorStatus);

    List<VendorUser> findByActiveTrue();

    long countByVendorStatus(VendorStatus vendorStatus);
}
