package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.CourierApplication;
import com.bolas.ecommerce.model.CourierApplicationStatus;
import com.bolas.ecommerce.model.VendorUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourierApplicationRepository extends JpaRepository<CourierApplication, Long> {
    List<CourierApplication> findByStatusOrderBySubmittedAtDesc(CourierApplicationStatus status);
    List<CourierApplication> findByStatusInOrderBySubmittedAtDesc(List<CourierApplicationStatus> statuses);
    List<CourierApplication> findByVendorOrderBySubmittedAtDesc(VendorUser vendor);
    long countByStatus(CourierApplicationStatus status);
    void deleteByVendor(VendorUser vendor);

    /** Fetch eager du vendor pour éviter LazyInitializationException dans admin/courier-activity */
    @Query("SELECT c FROM CourierApplication c LEFT JOIN FETCH c.vendor WHERE c.status IN :statuses ORDER BY c.submittedAt DESC")
    List<CourierApplication> findAllWithVendorByStatusIn(@Param("statuses") List<CourierApplicationStatus> statuses);
}
