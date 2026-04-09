package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CountryRepository extends JpaRepository<Country, Long> {
    List<Country> findByActiveTrueOrderByNameAsc();
    long countByActiveTrue();
    Optional<Country> findByCode(String code);
}
