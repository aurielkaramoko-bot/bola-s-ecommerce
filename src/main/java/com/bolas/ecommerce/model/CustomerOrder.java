package com.bolas.ecommerce.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shop_orders")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 32)
    @Column(nullable = false, unique = true, length = 32)
    private String trackingNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status = OrderStatus.PENDING;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false)
    private String customerName;

    @NotBlank
    @Size(max = 40)
    @Column(nullable = false, length = 40)
    private String customerPhone;

    @Size(max = 500)
    @Column(length = 500)
    private String customerAddress;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DeliveryOption deliveryOption;

    @Column
    private Double clientLatitude;

    @Column
    private Double clientLongitude;

    @Column
    private Double courierLatitude;

    @Column
    private Double courierLongitude;

    /** Token unique pour le suivi GPS automatique du livreur (UUID). */
    @Size(max = 64)
    @Column(length = 64, unique = true)
    private String courierToken;

    /** Pays de livraison : TG = Togo, CI = Côte d'Ivoire */
    @Size(max = 2)
    @Column(length = 2)
    private String country = "TG";

    /** Téléphone du livreur (affiché au client pour appel / WhatsApp). */
    @Size(max = 40)
    @Column(length = 40)
    private String courierPhone;

    /** Nom du livreur assigné par l'admin */
    @Size(max = 150)
    @Column(name = "assigned_courier_name", length = 150)
    private String assignedCourierName;

    /** Téléphone du livreur assigné */
    @Size(max = 40)
    @Column(name = "assigned_courier_phone", length = 40)
    private String assignedCourierPhone;

    /** Plaque d'immatriculation du véhicule de livraison. */
    @Size(max = 32)
    @Column(length = 32)
    private String courierVehiclePlate;

    /** URL photo livreur ou véhicule (/uploads/... ou lien externe). */
    @Size(max = 2000)
    @Column(length = 2000)
    private String courierPhotoUrl;

    @NotNull
    @Column(nullable = false)
    private Long totalAmountCfa;

    @Column(nullable = false)
    private long deliveryFeeCfa;

    /** Commission BOLA prélevée sur cette commande (en CFA) */
    @Column(nullable = false)
    private long commissionCfa = 0L;

    /** Taux de commission appliqué (en %) */
    @Column(nullable = false)
    private int commissionPercent = 0;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "customerOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderLine> lines = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }

    public DeliveryOption getDeliveryOption() {
        return deliveryOption;
    }

    public void setDeliveryOption(DeliveryOption deliveryOption) {
        this.deliveryOption = deliveryOption;
    }

    public Double getClientLatitude() {
        return clientLatitude;
    }

    public void setClientLatitude(Double clientLatitude) {
        this.clientLatitude = clientLatitude;
    }

    public Double getClientLongitude() {
        return clientLongitude;
    }

    public void setClientLongitude(Double clientLongitude) {
        this.clientLongitude = clientLongitude;
    }

    public Double getCourierLatitude() {
        return courierLatitude;
    }

    public void setCourierLatitude(Double courierLatitude) {
        this.courierLatitude = courierLatitude;
    }

    public Double getCourierLongitude() {
        return courierLongitude;
    }

    public void setCourierLongitude(Double courierLongitude) {
        this.courierLongitude = courierLongitude;
    }

    public String getCourierToken() { return courierToken; }
    public void setCourierToken(String courierToken) { this.courierToken = courierToken; }

    public String getCountry() { return country != null ? country : "TG"; }
    public void setCountry(String country) { this.country = country; }

    public String getCourierPhone() {
        return courierPhone;
    }

    public void setCourierPhone(String courierPhone) {
        this.courierPhone = courierPhone;
    }

    public String getCourierVehiclePlate() {
        return courierVehiclePlate;
    }

    public void setCourierVehiclePlate(String courierVehiclePlate) {
        this.courierVehiclePlate = courierVehiclePlate;
    }

    public String getCourierPhotoUrl() {
        return courierPhotoUrl;
    }

    public void setCourierPhotoUrl(String courierPhotoUrl) {
        this.courierPhotoUrl = courierPhotoUrl;
    }

    public String getAssignedCourierName() { return assignedCourierName; }
    public void setAssignedCourierName(String assignedCourierName) { this.assignedCourierName = assignedCourierName; }

    public String getAssignedCourierPhone() { return assignedCourierPhone; }
    public void setAssignedCourierPhone(String assignedCourierPhone) { this.assignedCourierPhone = assignedCourierPhone; }

    public Long getTotalAmountCfa() {
        return totalAmountCfa;
    }

    public void setTotalAmountCfa(Long totalAmountCfa) {
        this.totalAmountCfa = totalAmountCfa;
    }

    public long getDeliveryFeeCfa() { return deliveryFeeCfa; }
    public void setDeliveryFeeCfa(long deliveryFeeCfa) { this.deliveryFeeCfa = deliveryFeeCfa; }

    public long getCommissionCfa() { return commissionCfa; }
    public void setCommissionCfa(long commissionCfa) { this.commissionCfa = commissionCfa; }

    public int getCommissionPercent() { return commissionPercent; }
    public void setCommissionPercent(int commissionPercent) { this.commissionPercent = commissionPercent; }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<OrderLine> getLines() {
        return lines;
    }

    public void setLines(List<OrderLine> lines) {
        this.lines = lines;
    }

    public void addLine(OrderLine line) {
        lines.add(line);
        line.setCustomerOrder(this);
    }
}
