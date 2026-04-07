package com.bolas.ecommerce.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String firstName;

    @NotBlank @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String lastName;

    @NotBlank @Size(max = 200)
    @Column(nullable = false, unique = true, length = 200)
    private String email;

    /** Null si inscription via Google */
    @Size(max = 200)
    @Column(length = 200)
    private String passwordHash;

    @Size(max = 40)
    @Column(length = 40)
    private String phone;

    /** Date de naissance — utilisée pour le tracking number */
    @Column
    private LocalDate birthDate;

    /** ID Google OAuth2 — null si inscription classique */
    @Size(max = 200)
    @Column(length = 200, unique = true)
    private String googleId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    // --- Getters / Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    /** Prénom normalisé sans accents pour le tracking number */
    public String getFirstNameNormalized() {
        if (firstName == null) return "CLI";
        String s = firstName.toUpperCase()
                .replaceAll("[ÀÁÂÃÄÅ]", "A").replaceAll("[ÈÉÊË]", "E")
                .replaceAll("[ÌÍÎÏ]", "I").replaceAll("[ÒÓÔÕÖ]", "O")
                .replaceAll("[ÙÚÛÜ]", "U").replaceAll("[^A-Z]", "");
        return s.length() >= 3 ? s.substring(0, 3) : s.length() > 0 ? s : "CLI";
    }
}
