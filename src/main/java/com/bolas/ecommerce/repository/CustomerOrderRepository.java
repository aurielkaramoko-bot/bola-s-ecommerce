package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.CustomerOrder;
import com.bolas.ecommerce.model.OrderStatus;
import com.bolas.ecommerce.model.VendorUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    Optional<CustomerOrder> findByTrackingNumber(String trackingNumber);

    Optional<CustomerOrder> findByCourierToken(String courierToken);

    List<CustomerOrder> findTop10ByOrderByCreatedAtDesc();

    List<CustomerOrder> findAllByOrderByCreatedAtDesc();

    List<CustomerOrder> findByStatusOrderByCreatedAtAsc(OrderStatus status);

    List<CustomerOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    List<CustomerOrder> findTop20ByStatusInOrderByCreatedAtDesc(Collection<OrderStatus> statuses);

    /** Commandes contenant au moins un produit de ce vendeur, filtrées par statut */
    @Query("""
        SELECT DISTINCT o FROM CustomerOrder o
        JOIN o.lines l
        JOIN l.product p
        WHERE p.vendor = :vendor
        AND o.status IN :statuses
        ORDER BY o.createdAt ASC
        """)
    List<CustomerOrder> findByVendorProductsAndStatusIn(
            @Param("vendor") VendorUser vendor,
            @Param("statuses") Collection<OrderStatus> statuses);

    /** Compte le total d'articles commandés par un client (téléphone) chez un vendeur */
    @Query("""
        SELECT COALESCE(SUM(l.quantity), 0)
        FROM OrderLine l
        JOIN l.product p
        JOIN l.customerOrder o
        WHERE p.vendor = :vendor
        AND o.customerPhone = :phone
        AND o.status IN ('CONFIRMED','READY','IN_DELIVERY','DELIVERED')
        """)
    long countItemsByCustomerPhoneAndVendor(
            @Param("phone") String phone,
            @Param("vendor") VendorUser vendor);
}
