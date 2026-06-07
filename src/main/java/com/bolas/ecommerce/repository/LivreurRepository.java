package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.Livreur;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LivreurRepository extends JpaRepository<Livreur, Long> {
    Optional<Livreur> findByPhone(String phone);
    boolean existsByPhone(String phone);
}
