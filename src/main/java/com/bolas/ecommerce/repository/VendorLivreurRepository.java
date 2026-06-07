package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.Livreur;
import com.bolas.ecommerce.model.VendorLivreur;
import com.bolas.ecommerce.model.VendorUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VendorLivreurRepository extends JpaRepository<VendorLivreur, Long> {

    List<VendorLivreur> findByVendor(VendorUser vendor);

    List<VendorLivreur> findByVendorAndActiveTrue(VendorUser vendor);

    Optional<VendorLivreur> findByVendorAndLivreur(VendorUser vendor, Livreur livreur);

    boolean existsByVendorAndLivreur(VendorUser vendor, Livreur livreur);

    @Query("SELECT vl FROM VendorLivreur vl JOIN FETCH vl.livreur WHERE vl.vendor = :vendor")
    List<VendorLivreur> findByVendorWithLivreur(@Param("vendor") VendorUser vendor);
}
