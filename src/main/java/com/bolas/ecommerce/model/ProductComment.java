package com.bolas.ecommerce.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.Instant;

@Entity
@Table(name = "product_comments")
public class ProductComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Null si commentaire racine, non-null si réponse */
    @Column(name = "parent_id")
    private Long parentId;

    /** Nom affiché (peut être anonyme si non connecté — non, on exige connexion pour poster) */
    @Size(max = 100)
    @Column(name = "author_name", length = 100)
    private String authorName;

    /** ID client connecté (null si vendeur ou admin) */
    @Column(name = "customer_id")
    private Long customerId;

    /** ID vendeur si c'est une réponse vendeur */
    @Column(name = "vendor_id")
    private Long vendorId;

    @Size(max = 1000)
    @Column(nullable = false, length = 1000)
    private String text;

    @Column(name = "likes_count")
    private int likesCount = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    /** True si l'auteur a bien acheté ce produit */
    @Column(name = "verified_buyer")
    private boolean verifiedBuyer = false;

    /** True si supprimé par modération (garde la structure de réponses) */
    @Column
    private boolean deleted = false;

    /** URL photo Cloudinary optionnelle */
    @Size(max = 2000)
    @Column(name = "photo_url", length = 2000)
    private String photoUrl;

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public boolean isVerifiedBuyer() { return verifiedBuyer; }
    public void setVerifiedBuyer(boolean verifiedBuyer) { this.verifiedBuyer = verifiedBuyer; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getCreatedAtFormatted() {
        if (createdAt == null) return "";
        long diff = (Instant.now().toEpochMilli() - createdAt.toEpochMilli()) / 60000;
        if (diff < 1) return "à l'instant";
        if (diff < 60) return diff + " min";
        if (diff < 1440) return (diff / 60) + "h";
        if (diff < 10080) return (diff / 1440) + "j";
        return (diff / 10080) + " sem.";
    }

    public boolean isVendorComment() { return vendorId != null; }
}
