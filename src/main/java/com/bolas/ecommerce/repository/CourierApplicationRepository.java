package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.CourierApplication;
import com.bolas.ecommerce.model.CourierApplicationStatus;
import com.bolas.ecommerce.model.VendorUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourierApplicationRepository extends JpaRepository<CourierApplication, Long> {
    List<CourierApplication> findByStatusOrderBySubmittedAtDesc(CourierApplicationStatus status);
    List<CourierApplication> findByVendorOrderBySubmittedAtDesc(VendorUser vendor);
    long countByStatus(CourierApplicationStatus status);
    void deleteByVendor(VendorUser vendor);
}
