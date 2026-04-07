package com.bolas.ecommerce.config;

import com.bolas.ecommerce.security.LoginAttemptRepository;
import com.bolas.ecommerce.model.AdminUser;
import com.bolas.ecommerce.model.Category;
import com.bolas.ecommerce.model.CustomerOrder;
import com.bolas.ecommerce.model.DeliveryOption;
import com.bolas.ecommerce.model.OrderLine;
import com.bolas.ecommerce.model.OrderStatus;
import com.bolas.ecommerce.model.Product;
import com.bolas.ecommerce.repository.AdminUserRepository;
import com.bolas.ecommerce.repository.CategoryRepository;
import com.bolas.ecommerce.repository.CustomerOrderRepository;
import com.bolas.ecommerce.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

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
            Environment environment,
            @Value("${bolas.admin.username:admin}") String adminUsername,
            @Value("${bolas.admin.password:}") String adminPassword) {
        return args -> {
            // Nettoie tous les blocages de login au démarrage
            loginAttemptRepository.deleteAll();
            boolean prod = isProd(environment);
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
                // Synchronise toujours le premier admin avec les variables d'environnement
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

            if (categoryRepository.count() > 0) {
                return;
            }

            Category c1 = cat(categoryRepository, "Artisanat local", "Créations artisanales et décoration.",
                    "https://images.unsplash.com/photo-1590874103328-eac38a683ce7?w=800");
            Category c2 = cat(categoryRepository, "Mode & accessoires", "Sacs, bijoux et textiles.",
                    "https://images.unsplash.com/photo-1548036328-c9fa89d128fa?w=800");
            Category c3 = cat(categoryRepository, "Maison & cuisine", "Ustensiles et art de la table.",
                    "https://images.unsplash.com/photo-1610701596007-11502848f5a3?w=800");
            Category c4 = cat(categoryRepository, "Beauté & bien-être", "Soins naturels.",
                    "https://images.unsplash.com/photo-1608571423902-eed4a5ad8108?w=800");
            Category c5 = cat(categoryRepository, "Enfants", "Jouets et vêtements.",
                    "https://images.unsplash.com/photo-1558060370-d644479cb6f7?w=800");

            Product p1 = prod("Panier tressé main", "Panier en jonc de qualité.", 25000L, 19900L, c1,
                    "https://images.unsplash.com/photo-1590874103328-eac38a683ce7?w=800", true, true, 2000, true);
            Product p2 = prod("Tapis berbère", "Tapis tissé à la main.", 120000L, null, c1,
                    "https://images.unsplash.com/photo-1600166898405-2558506a5c7f?w=800", true, false, 0, true);
            Product p3 = prod("Sac cuir camel", "Sac à main en cuir pleine fleur.", 85000L, 72000L, c2,
                    "https://images.unsplash.com/photo-1548036328-c9fa89d128fa?w=800", true, true, 3000, true);
            Product p4 = prod("Collier argent", "Collier artisanal.", 18000L, null, c2,
                    "https://images.unsplash.com/photo-1515562146424-2a0e05b5016d?w=800", true, true, 1500, false);
            Product p5 = prod("Plat en terre cuite", "Plat traditionnel émaillé.", 15000L, null, c3,
                    "https://images.unsplash.com/photo-1610701596007-11502848f5a3?w=800", true, true, 2500, false);
            Product p6 = prod("Théière berbère", "Théière gravée main.", 32000L, 27900L, c3,
                    "https://images.unsplash.com/photo-1577930330093-e787d1b679b6?w=800", true, true, 2000, true);
            Product p7 = prod("Huile d'argan bio", "100 ml pressée à froid.", 12000L, null, c4,
                    "https://images.unsplash.com/photo-1608571423902-eed4a5ad8108?w=800", true, true, 1000, false);
            Product p8 = prod("Savon noir", "Pot 200 g.", 4500L, null, c4,
                    "https://images.unsplash.com/photo-1556228578-8c89e6adf883?w=800", true, false, 0, false);
            Product p9 = prod("Jouet bois", "Jouet éducatif.", 8000L, 6500L, c5,
                    "https://images.unsplash.com/photo-1558060370-d644479cb6f7?w=800", true, true, 1500, true);
            Product p10 = prod("Body bébé coton", "Coton bio.", 7000L, null, c5,
                    "https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?w=800", true, true, 2000, false);

            productRepository.save(p1);
            productRepository.save(p2);
            productRepository.save(p3);
            productRepository.save(p4);
            productRepository.save(p5);
            productRepository.save(p6);
            productRepository.save(p7);
            productRepository.save(p8);
            productRepository.save(p9);
            productRepository.save(p10);

            CustomerOrder o1 = order("Client Démo", "22870111222", "Abidjan, Cocody", DeliveryOption.HOME,
                    5.35, -3.99, OrderStatus.IN_DELIVERY, 41900L, 2000L, p1);
            CustomerOrder o2 = order("Awa K.", "22870333444", "Abidjan, Marcory", DeliveryOption.HOME,
                    5.28, -4.02, OrderStatus.PENDING, 72000L, 3000L, p3);
            customerOrderRepository.save(o1);
            customerOrderRepository.save(o2);
        };
    }

    private static boolean isProd(Environment environment) {
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    private static Category cat(CategoryRepository r, String name, String desc, String imageUrl) {
        Category c = new Category();
        c.setName(name);
        c.setDescription(desc);
        c.setImageUrl(imageUrl);
        return r.save(c);
    }

    private static Product prod(String name, String desc, long price, Long promo,
                                Category cat, String img, boolean avail, boolean deliv, long delFee, boolean feat) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(desc);
        p.setPriceCfa(price);
        p.setPromoPriceCfa(promo);
        p.setCategory(cat);
        p.setImageUrl(img);
        p.setAvailable(avail);
        p.setDeliveryAvailable(deliv);
        p.setDeliveryPriceCfa(delFee);
        p.setFeatured(feat);
        return p;
    }

    private static CustomerOrder order(String name, String phone, String addr,
                                       DeliveryOption opt, double clat, double clng, OrderStatus st,
                                       long total, long fee, Product product) {
        CustomerOrder o = new CustomerOrder();
        o.setTrackingNumber("BOL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        o.setStatus(st);
        o.setCustomerName(name);
        o.setCustomerPhone(phone);
        o.setCustomerAddress(addr);
        o.setDeliveryOption(opt);
        o.setClientLatitude(clat);
        o.setClientLongitude(clng);
        o.setCourierLatitude(clat + 0.02);
        o.setCourierLongitude(clng + 0.01);
        o.setTotalAmountCfa(total);
        o.setDeliveryFeeCfa(fee);

        OrderLine line = new OrderLine();
        line.setProduct(product);
        line.setQuantity(1);
        line.setUnitPriceCfa(product.getEffectivePriceCfa());
        o.addLine(line);
        return o;
    }
}
