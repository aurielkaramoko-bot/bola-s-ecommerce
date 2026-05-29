package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.PriceChangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PriceChangeHistoryRepository extends JpaRepository<PriceChangeHistory, Long> {
    List<PriceChangeHistory> findTop20ByOrderByChangedAtDesc();
}
