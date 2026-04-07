package com.bolas.ecommerce.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "vendor_users")
public class VendorUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Size(max = 100)
    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @NotBlank @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String passwordHash;

    @NotBlank @Size(max = 40)
    @Column(nullable = false, length = 40)
    private String phone;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
