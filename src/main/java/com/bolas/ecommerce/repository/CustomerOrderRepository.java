package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.CustomerOrder;
import com.bolas.ecommerce.model.OrderStatus;
import com.bolas.ecommerce.model.VendorUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    Optional<CustomerOrder> findByTrackingNumber(String trackingNumber);

    Optional<CustomerOrder> findByCourierToken(String courierToken);

    List<CustomerOrder> findTop10ByOrderByCreatedAtDesc();

    List<CustomerOrder> findAllByOrderByCreatedAtDesc();

    /** Pageable version — replaces findAllByOrderByCreatedAtDesc for large datasets */
    Page<CustomerOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<CustomerOrder> findByStatusOrderByCreatedAtAsc(OrderStatus status);

    List<CustomerOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    /** Pageable version for status queries */
    Page<CustomerOrder> findByStatusOrderByCreatedAtAsc(OrderStatus status, Pageable pageable);

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

    /** Commandes directement liées à un vendeur (via vendor_id sur la commande) */
    List<CustomerOrder> findByVendorAndStatusInOrderByCreatedAtDesc(
            VendorUser vendor, Collection<OrderStatus> statuses);

    /** Toutes les commandes d'un vendeur */
    List<CustomerOrder> findByVendorOrderByCreatedAtDesc(VendorUser vendor);

    /** Pageable version pour les commandes d'un vendeur */
    Page<CustomerOrder> findByVendorOrderByCreatedAtDesc(VendorUser vendor, Pageable pageable);

    /** Nombre de commandes d'un vendeur par statut */
    long countByVendorAndStatus(VendorUser vendor, OrderStatus status);

    /** Nombre total de commandes d'un vendeur */
    long countByVendor(VendorUser vendor);

    /** CA d'un vendeur sur une période */
    @Query("""
        SELECT COALESCE(SUM(o.totalAmountCfa), 0) FROM CustomerOrder o
        WHERE o.vendor = :vendor
        AND o.status IN ('CONFIRMED','READY','IN_DELIVERY','DELIVERED')
        AND o.createdAt >= :start AND o.createdAt <= :end
        """)
    long sumRevenueByVendorBetween(
            @Param("vendor") VendorUser vendor,
            @Param("start") Instant start,
            @Param("end") Instant end);

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

    /** Compte par statut (optimisé : pas de chargement en mémoire) */
    long countByStatus(OrderStatus status);

    /** Compte les commandes dans une période (analytics optimisé) */
    @Query("SELECT COUNT(o) FROM CustomerOrder o WHERE o.createdAt >= :start AND o.createdAt <= :end")
    long countByCreatedAtBetween(@Param("start") Instant start, @Param("end") Instant end);

    /** Top 50 commandes récentes — pour la page livraison admin (évite de charger tout) */
    List<CustomerOrder> findTop50ByOrderByCreatedAtDesc();

    /** Commandes actives bornées par statut — évite de charger tout en mémoire */
    List<CustomerOrder> findTop100ByStatusOrderByCreatedAtAsc(OrderStatus status);
    List<CustomerOrder> findTop100ByStatusOrderByCreatedAtDesc(OrderStatus status);

    /** Commandes avec lignes eager-fetched — pour l'admin orders page */
    @Query("""
        SELECT DISTINCT o FROM CustomerOrder o
        LEFT JOIN FETCH o.lines l
        LEFT JOIN FETCH l.product p
        LEFT JOIN FETCH p.vendor
        WHERE o.status = :status
        ORDER BY o.createdAt ASC
        """)
    List<CustomerOrder> findTop100ByStatusWithLinesAsc(@Param("status") OrderStatus status);

    @Query("""
        SELECT DISTINCT o FROM CustomerOrder o
        LEFT JOIN FETCH o.lines l
        LEFT JOIN FETCH l.product p
        LEFT JOIN FETCH p.vendor
        WHERE o.status = :status
        ORDER BY o.createdAt DESC
        """)
    List<CustomerOrder> findTop100ByStatusWithLinesDesc(@Param("status") OrderStatus status);

    /** Commandes vendeur avec lignes eager-fetched */
    @Query("""
        SELECT DISTINCT o FROM CustomerOrder o
        LEFT JOIN FETCH o.lines l
        LEFT JOIN FETCH l.product p
        WHERE o.vendor = :vendor
        AND o.status IN :statuses
        ORDER BY o.createdAt DESC
        """)
    List<CustomerOrder> findByVendorAndStatusInWithLines(
            @Param("vendor") VendorUser vendor,
            @Param("statuses") Collection<OrderStatus> statuses);

    /** Toutes les commandes d'un vendeur avec lignes eager-fetched */
    @Query("""
        SELECT DISTINCT o FROM CustomerOrder o
        LEFT JOIN FETCH o.lines l
        LEFT JOIN FETCH l.product p
        WHERE o.vendor = :vendor
        ORDER BY o.createdAt DESC
        """)
    List<CustomerOrder> findByVendorWithLines(@Param("vendor") VendorUser vendor);

    /** Commande par ID avec lignes eager-fetched */
    @Query("""
        SELECT o FROM CustomerOrder o
        LEFT JOIN FETCH o.lines l
        LEFT JOIN FETCH l.product p
        LEFT JOIN FETCH p.vendor
        WHERE o.id = :id
        """)
    java.util.Optional<CustomerOrder> findByIdWithLines(@Param("id") Long id);

    /** Nombre de livraisons effectuées par un livreur (par nom) */
    long countByAssignedCourierName(String assignedCourierName);

    /** Nombre de livraisons effectuées par un livreur dans un statut donné */
    long countByAssignedCourierNameAndStatus(String assignedCourierName, OrderStatus status);

    /** Dernières commandes assignées à un livreur */
    List<CustomerOrder> findTop10ByAssignedCourierNameOrderByCreatedAtDesc(String assignedCourierName);
}

