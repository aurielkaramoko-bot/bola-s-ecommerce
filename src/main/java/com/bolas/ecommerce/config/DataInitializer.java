package com.bolas.ecommerce.config;

import com.bolas.ecommerce.security.LoginAttemptRepository;
import com.bolas.ecommerce.model.AdminUser;
import com.bolas.ecommerce.model.Country;
import com.bolas.ecommerce.repository.AdminUserRepository;
import com.bolas.ecommerce.repository.CategoryRepository;
import com.bolas.ecommerce.repository.CountryRepository;
import com.bolas.ecommerce.repository.CustomerOrderRepository;
import com.bolas.ecommerce.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

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
            @Value("${bolas.admin.password:}") String adminPasswordRaw) {
        return args -> {
            loginAttemptRepository.deleteAll();
            boolean prod = isProd(environment);

            // Résoudre le mot de passe final (non réassignable pour les lambdas)
            final String resolvedPassword = resolveAdminPassword(adminPasswordRaw, prod);
            final String resolvedUsername = (adminUsername != null && !adminUsername.isBlank())
                    ? adminUsername.trim() : "admin";

            // ── Admin ────────────────────────────────────────────────────────
            if (adminUserRepository.count() == 0) {
                AdminUser admin = new AdminUser();
                admin.setUsername(resolvedUsername);
                admin.setPasswordHash(passwordEncoder.encode(resolvedPassword));
                adminUserRepository.save(admin);
                log.info("Admin créé : {}", resolvedUsername);
            } else {
                // Synchronise uniquement si un vrai mot de passe est fourni
                adminUserRepository.findAll().stream().findFirst().ifPresent(admin -> {
                    boolean usernameChanged = !admin.getUsername().equals(resolvedUsername);
                    boolean passwordChanged = !passwordEncoder.matches(resolvedPassword, admin.getPasswordHash());
                    if (usernameChanged || passwordChanged) {
                        admin.setUsername(resolvedUsername);
                        admin.setPasswordHash(passwordEncoder.encode(resolvedPassword));
                        adminUserRepository.save(admin);
                        log.info("Admin mis à jour : {}", resolvedUsername);
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

            // ── Catégories ────────────────────────────────────────────────────
            categorySeeder.seed();
        };
    }

    /**
     * Résout le mot de passe admin :
     * - En prod sans ADMIN_PASSWORD → exception (déploiement bloqué intentionnellement)
     * - En dev sans ADMIN_PASSWORD → fallback local
     */
    private static String resolveAdminPassword(String raw, boolean prod) {
        if (raw != null && !raw.isBlank()) {
            if (prod && raw.length() < 12) {
                throw new IllegalStateException(
                        "ADMIN_PASSWORD doit contenir au moins 12 caractères en production.");
            }
            return raw;
        }
        if (prod) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD est obligatoire en production. "
                    + "Configurez la variable d'environnement sur Render.");
        }
        log.warn("ADMIN_PASSWORD non défini — utilisation du mot de passe dev local (non sécurisé)");
        return "admin_dev_local_only";
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
