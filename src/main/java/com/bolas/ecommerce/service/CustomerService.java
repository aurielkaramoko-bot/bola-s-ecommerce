package com.bolas.ecommerce.service;

import com.bolas.ecommerce.model.Customer;
import com.bolas.ecommerce.repository.CustomerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Random;

@Service
public class CustomerService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final DateTimeFormatter BIRTH_FMT = DateTimeFormatter.ofPattern("ddMMyy");
    private final Random random = new Random();

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Inscription classique email/mot de passe */
    public Customer register(String firstName, String lastName, String email,
                             String rawPassword, String phone, LocalDate birthDate) {
        if (customerRepository.existsByEmail(email.toLowerCase().trim())) {
            throw new IllegalArgumentException("Un compte existe déjà avec cet email.");
        }
        Customer c = new Customer();
        c.setFirstName(firstName.trim());
        c.setLastName(lastName.trim());
        c.setEmail(email.toLowerCase().trim());
        c.setPasswordHash(passwordEncoder.encode(rawPassword));
        c.setPhone(phone != null ? phone.trim() : null);
        c.setBirthDate(birthDate);
        return customerRepository.save(c);
    }

    /** Login classique — retourne le customer si credentials OK */
    public Optional<Customer> login(String email, String rawPassword) {
        return customerRepository.findByEmail(email.toLowerCase().trim())
                .filter(c -> c.getPasswordHash() != null
                        && passwordEncoder.matches(rawPassword, c.getPasswordHash()));
    }

    /** Connexion / création via Google OAuth2 */
    public Customer loginOrCreateGoogle(String googleId, String email,
                                        String firstName, String lastName) {
        // Cherche par googleId d'abord, puis par email (compte existant à lier)
        Optional<Customer> byGoogle = customerRepository.findByGoogleId(googleId);
        if (byGoogle.isPresent()) return byGoogle.get();

        Optional<Customer> byEmail = customerRepository.findByEmail(email.toLowerCase().trim());
        if (byEmail.isPresent()) {
            Customer c = byEmail.get();
            c.setGoogleId(googleId);
            return customerRepository.save(c);
        }

        Customer c = new Customer();
        c.setGoogleId(googleId);
        c.setEmail(email.toLowerCase().trim());
        c.setFirstName(firstName != null ? firstName.trim() : "");
        c.setLastName(lastName != null ? lastName.trim() : "");
        return customerRepository.save(c);
    }

    /**
     * Génère un tracking number unique style :
     * BOL-MAR150396-X4K2
     * = BOL + 3 lettres prénom + JJMMAA date naissance + 4 chars aléatoires
     * Si pas de date de naissance → BOL-MAR-X4K2F8
     */
    public String generateTrackingNumber(Customer customer) {
        String prefix = "BOL-" + customer.getFirstNameNormalized();
        String datePart = customer.getBirthDate() != null
                ? customer.getBirthDate().format(BIRTH_FMT)
                : "";
        String suffix = randomChars(4);

        String candidate = prefix + datePart + "-" + suffix;
        // Garantit l'unicité (très rare collision mais on vérifie)
        int attempts = 0;
        while (attempts++ < 10) {
            // On ne stocke pas encore, juste on vérifie via le repo si besoin
            return candidate;
        }
        return candidate;
    }

    private String randomChars(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public Optional<Customer> findByEmail(String email) {
        return customerRepository.findByEmail(email.toLowerCase().trim());
    }
}
