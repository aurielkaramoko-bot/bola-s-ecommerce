package com.bolas.ecommerce.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Message de chat entre un acheteur et un vendeur.
 */
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private VendorUser vendor;

    /** Email ou téléphone du client (identifiant unique côté acheteur) */
    @NotBlank @Size(max = 200)
    @Column(name = "customer_identifier", nullable = false, length = 200)
    private String customerIdentifier;

    /** Nom affiché du client */
    @Size(max = 150)
    @Column(name = "customer_name", length = 150)
    private String customerName;

    /** "CUSTOMER" ou "VENDOR" */
    @NotBlank @Size(max = 10)
    @Column(name = "sender_type", nullable = false, length = 10)
    private String senderType;

    @NotBlank @Size(max = 2000)
    @Column(nullable = false, length = 2000)
    private String message;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt = Instant.now();

    @Column(name = "read_by_vendor")
    private boolean readByVendor = false;

    @Column(name = "read_by_customer")
    private boolean readByCustomer = false;

    // ─── Getters / Setters ────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public VendorUser getVendor() { return vendor; }
    public void setVendor(VendorUser vendor) { this.vendor = vendor; }

    public String getCustomerIdentifier() { return customerIdentifier; }
    public void setCustomerIdentifier(String customerIdentifier) { this.customerIdentifier = customerIdentifier; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getSenderType() { return senderType; }
    public void setSenderType(String senderType) { this.senderType = senderType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }

    public boolean isReadByVendor() { return readByVendor; }
    public void setReadByVendor(boolean readByVendor) { this.readByVendor = readByVendor; }

    public boolean isReadByCustomer() { return readByCustomer; }
    public void setReadByCustomer(boolean readByCustomer) { this.readByCustomer = readByCustomer; }
}
