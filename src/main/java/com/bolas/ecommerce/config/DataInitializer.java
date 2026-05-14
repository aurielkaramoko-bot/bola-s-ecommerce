package com.bolas.ecommerce.config;

import com.bolas.ecommerce.security.LoginAttemptRepository;
import com.bolas.ecommerce.model.AdminUser;
import com.bolas.ecommerce.model.Country;
import com.bolas.ecommerce.repository.AdminUserRepository;
import com.bolas.ecommerce.repository.CategoryRepository;
import com.bolas.ecommerce.repository.CountryRepository;
import com.bolas.ecommerce.repository.CustomerOrderRepository;
import com.bolas.ecommerce.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    @Order(1)
    CommandLineRunner seedData(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            CustomerOrderRepository customerOrderRepository,
            LoginAttemptRepository loginAttemptRepository,
            CountryRepository countryRepository,
            CategorySeeder categorySeeder,
            Environment environment,
            @Value("${bolas.admin.username:admin}") String adminUsername,
            @Value("${bolas.admin.password:}") String adminPassword) {
        return args -> {
            // Nettoie tous les blocages de login au démarrage
            loginAttemptRepository.deleteAll();
            boolean prod = isProd(environment);

            // ── Admin ────────────────────────────────────────────────────────
            if (adminUserRepository.count() == 0) {
                if (adminPassword == null || adminPassword.isBlank()) {
                    throw new IllegalStateException("ADMIN_PASSWORD requis pour initialiser l'admin.");
                }
                if (prod && adminPassword.length() < 12) {
                    throw new IllegalStateException("ADMIN_PASSWORD doit contenir au moins 12 caractères en production.");
                }
                AdminUser admin = new AdminUser();
                admin.setUsername(adminUsername.trim());
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                adminUserRepository.save(admin);
            } else {
                // Synchronise le premier admin avec les variables d'environnement
                adminUserRepository.findAll().stream().findFirst().ifPresent(admin -> {
                    boolean usernameChanged = !admin.getUsername().equals(adminUsername.trim());
                    boolean passwordChanged = !passwordEncoder.matches(adminPassword, admin.getPasswordHash());
                    if (usernameChanged || passwordChanged) {
                        admin.setUsername(adminUsername.trim());
                        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                        adminUserRepository.save(admin);
                    }
                });
            }

            // ── Pays par défaut ───────────────────────────────────────────────
            ensureCountry(countryRepository, "TG", "Togo",          "🇹🇬");
            ensureCountry(countryRepository, "CI", "Côte d'Ivoire", "🇨🇮");
            ensureCountry(countryRepository, "GA", "Gabon",         "🇬🇦");
            ensureCountry(countryRepository, "SN", "Sénégal",       "🇸🇳");
            ensureCountry(countryRepository, "BJ", "Bénin",         "🇧🇯");
            ensureCountry(countryRepository, "GH", "Ghana",         "🇬🇭");
            ensureCountry(countryRepository, "CM", "Cameroun",      "🇨🇲");
            ensureCountry(countryRepository, "NG", "Nigeria",       "🇳🇬");
            ensureCountry(countryRepository, "ML", "Mali",          "🇲🇱");
            ensureCountry(countryRepository, "BF", "Burkina Faso",  "🇧🇫");

            // ── Catégories : 21 grandes familles africaines ───────────────────
            categorySeeder.seed();
        };
    }

    private static void ensureCountry(CountryRepository repo, String code, String name, String flag) {
        if (repo.findByCode(code).isEmpty()) {
            Country c = new Country();
            c.setCode(code); c.setName(name); c.setFlag(flag);
            c.setCustomsTaxPercent(0); c.setActive(true);
            repo.save(c);
        }
    }

    private static boolean isProd(Environment environment) {
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile)) return true;
        }
        return false;
    }
}
