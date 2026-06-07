package com.bolas.ecommerce.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

@Configuration
public class DatabaseMigrationConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationConfig.class);

    @Bean
    @Order(0) // Avant DataInitializer
    CommandLineRunner migrateDatabase(DataSource dataSource, Environment environment) {
        return args -> {
            boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
            if (!prod) return;

            // Utiliser une connexion séparée qui ne perturbe pas le pool JPA
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(true);

                // Migration 1 : contrainte status
                try {
                    conn.createStatement().execute(
                        "ALTER TABLE shop_orders DROP CONSTRAINT IF EXISTS shop_orders_status_check"
                    );
                    log.info("Migration: contrainte shop_orders_status_check supprimée");
                } catch (SQLException e) {
                    log.warn("Migration: {}", e.getMessage());
                }
                try {
                    conn.createStatement().execute(
                        "ALTER TABLE shop_orders ADD CONSTRAINT shop_orders_status_check " +
                        "CHECK (status IN ('PENDING','CONFIRMED','READY','IN_DELIVERY','DELIVERED','CANCELLED'))"
                    );
                    log.info("Migration: nouvelle contrainte shop_orders_status_check créée avec succès");
                } catch (SQLException e) {
                    log.warn("Migration: contrainte déjà à jour: {}", e.getMessage());
                }

                // Migration 2 : colonne google_id sur customers
                try {
                    conn.createStatement().execute(
                        "ALTER TABLE customers ADD COLUMN IF NOT EXISTS google_id VARCHAR(200)"
                    );
                    log.info("Migration: colonne google_id ajoutée sur customers");
                } catch (SQLException e) {
                    log.warn("Migration google_id: {}", e.getMessage());
                }

                // Migration 3 : colonne country sur shop_orders
                try {
                    conn.createStatement().execute(
                        "ALTER TABLE shop_orders ADD COLUMN IF NOT EXISTS country VARCHAR(2) DEFAULT 'TG'"
                    );
                    log.info("Migration: colonne country ajoutée sur shop_orders");
                } catch (SQLException e) {
                    log.warn("Migration country: {}", e.getMessage());
                }

                // Migration 4 : colonne video_url sur products
                try {
                    conn.createStatement().execute(
                        "ALTER TABLE products ADD COLUMN IF NOT EXISTS video_url VARCHAR(2000)"
                    );
                    log.info("Migration: colonne video_url ajoutée sur products");
                } catch (SQLException e) {
                    log.warn("Migration video_url: {}", e.getMessage());
                }

                // Migration 5 : colonnes vendor_users (nouvelles)
                String[][] vendorCols = {
                    {"id_document_url",  "VARCHAR(2000)"},
                    {"requested_niche",  "VARCHAR(300)"},
                    {"id_doc_verified",  "BOOLEAN"},
                    {"suspension_reason","VARCHAR(500)"},
                    {"soft_suspend",     "BOOLEAN DEFAULT TRUE"},
                    {"banner_url",       "VARCHAR(2000)"},
                    {"vendor_status",    "VARCHAR(16) DEFAULT 'PENDING'"},
                };
                for (String[] col : vendorCols) {
                    try {
                        conn.createStatement().execute(
                            "ALTER TABLE vendor_users ADD COLUMN IF NOT EXISTS " + col[0] + " " + col[1]
                        );
                        log.info("Migration: colonne {} ajoutée sur vendor_users", col[0]);
                    } catch (SQLException e) {
                        log.warn("Migration vendor_users.{}: {}", col[0], e.getMessage());
                    }
                }

                // Migration 6 : colonnes shop_orders (nouvelles)
                String[][] orderCols = {
                    {"commission_cfa",         "BIGINT DEFAULT 0"},
                    {"commission_percent",      "INTEGER DEFAULT 0"},
                    {"courier_token",           "VARCHAR(64)"},
                    {"courier_phone",           "VARCHAR(40)"},
                    {"courier_vehicle_plate",   "VARCHAR(32)"},
                    {"courier_photo_url",       "VARCHAR(2000)"},
                    {"delivery_fee_cfa",        "BIGINT DEFAULT 0"},
                    {"client_latitude",         "DOUBLE"},
                    {"client_longitude",        "DOUBLE"},
                    {"courier_latitude",        "DOUBLE"},
                    {"courier_longitude",       "DOUBLE"},
                };
                for (String[] col : orderCols) {
                    try {
                        conn.createStatement().execute(
                            "ALTER TABLE shop_orders ADD COLUMN IF NOT EXISTS " + col[0] + " " + col[1]
                        );
                        log.info("Migration: colonne {} ajoutée sur shop_orders", col[0]);
                    } catch (SQLException e) {
                        log.warn("Migration shop_orders.{}: {}", col[0], e.getMessage());
                    }
                }

                // Migration 7 : table courier_applications
                try {
                    conn.createStatement().execute("""
                        CREATE TABLE IF NOT EXISTS courier_applications (
                            id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                            courier_name VARCHAR(150) NOT NULL,
                            courier_phone VARCHAR(40) NOT NULL,
                            zone VARCHAR(200),
                            photo_url VARCHAR(2000),
                            id_document_url VARCHAR(2000),
                            id_doc_verified BOOLEAN,
                            vendor_id BIGINT,
                            status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
                            submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                    """);
                    log.info("Migration: table courier_applications créée/vérifiée");
                } catch (SQLException e) {
                    log.warn("Migration courier_applications: {}", e.getMessage());
                }

                // Migration 8 : table countries
                try {
                    conn.createStatement().execute("""
                        CREATE TABLE IF NOT EXISTS countries (
                            id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                            code VARCHAR(4) NOT NULL UNIQUE,
                            name VARCHAR(100) NOT NULL,
                            flag VARCHAR(10),
                            customs_tax_percent INTEGER DEFAULT 0,
                            active BOOLEAN NOT NULL DEFAULT TRUE
                        )
                    """);
                    log.info("Migration: table countries créée/vérifiée");
                } catch (SQLException e) {
                    log.warn("Migration countries: {}", e.getMessage());
                }

                // Migration 9 : colonne sponsored sur products
                try {
                    conn.createStatement().execute(
                        "ALTER TABLE products ADD COLUMN IF NOT EXISTS sponsored BOOLEAN DEFAULT FALSE"
                    );
                    log.info("Migration: colonne sponsored ajoutée sur products");
                } catch (SQLException e) {
                    log.warn("Migration products.sponsored: {}", e.getMessage());
                }

                // Migration 10 : colonne delivery_price_cfa sur products
                try {
                    conn.createStatement().execute(
                        "ALTER TABLE products ADD COLUMN IF NOT EXISTS delivery_price_cfa BIGINT DEFAULT 0"
                    );
                    log.info("Migration: colonne delivery_price_cfa ajoutée sur products");
                } catch (SQLException e) {
                    log.warn("Migration products.delivery_price_cfa: {}", e.getMessage());
                }

                // Migration 11 : colonne limited_stock sur products
                try {
                    conn.createStatement().execute(
                        "ALTER TABLE products ADD COLUMN IF NOT EXISTS limited_stock BOOLEAN DEFAULT FALSE"
                    );
                    log.info("Migration: colonne limited_stock ajoutée sur products");
                } catch (SQLException e) {
                    log.warn("Migration products.limited_stock: {}", e.getMessage());
                }

                // Migration 12 : colonnes assigned_courier sur shop_orders
                String[][] assignedCols = {
                    {"assigned_courier_name",  "VARCHAR(150)"},
                    {"assigned_courier_phone", "VARCHAR(40)"},
                };
                for (String[] col : assignedCols) {
                    try {
                        conn.createStatement().execute(
                            "ALTER TABLE shop_orders ADD COLUMN IF NOT EXISTS " + col[0] + " " + col[1]
                        );
                        log.info("Migration: colonne {} ajoutée sur shop_orders", col[0]);
                    } catch (SQLException e) {
                        log.warn("Migration shop_orders.{}: {}", col[0], e.getMessage());
                    }
                }

                // Migration 13 : colonnes manquantes sur courier_applications
                String[][] courierCols = {
                    {"suspension_reason", "VARCHAR(500)"},
                    {"last_action_at",    "TIMESTAMP"},
                };
                for (String[] col : courierCols) {
                    try {
                        conn.createStatement().execute(
                            "ALTER TABLE courier_applications ADD COLUMN IF NOT EXISTS " + col[0] + " " + col[1]
                        );
                        log.info("Migration: colonne {} ajoutée sur courier_applications", col[0]);
                    } catch (SQLException e) {
                        log.warn("Migration courier_applications.{}: {}", col[0], e.getMessage());
                    }
                }

                // Migration 14 : colonne subscription_expires_at sur vendor_users
                try {
                    conn.createStatement().execute(
                        "ALTER TABLE vendor_users ADD COLUMN IF NOT EXISTS subscription_expires_at DATE"
                    );
                    log.info("Migration: colonne subscription_expires_at ajoutée sur vendor_users");
                } catch (SQLException e) {
                    log.warn("Migration vendor_users.subscription_expires_at: {}", e.getMessage());
                }
                try {
                    conn.createStatement().execute(
                        "ALTER TABLE vendor_users ADD COLUMN IF NOT EXISTS subscription_starts_at DATE"
                    );
                    log.info("Migration: colonne subscription_starts_at ajoutée sur vendor_users");
                } catch (SQLException e) {
                    log.warn("Migration vendor_users.subscription_starts_at: {}", e.getMessage());
                }

                // Migration 15 : table reports
                try {
                    conn.createStatement().execute("""
                        CREATE TABLE IF NOT EXISTS reports (
                            id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                            target_type VARCHAR(20) NOT NULL,
                            target_id BIGINT NOT NULL,
                            target_name VARCHAR(200),
                            reporter_email VARCHAR(200),
                            reason VARCHAR(500) NOT NULL,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            resolved BOOLEAN NOT NULL DEFAULT FALSE
                        )
                    """);
                    log.info("Migration: table reports créée/vérifiée");
                } catch (SQLException e) {
                    log.warn("Migration reports: {}", e.getMessage());
                }

                // Migration 16 : table app_settings (paramètres persistés : prix des packs, etc.)
                try {
                    conn.createStatement().execute("""
                        CREATE TABLE IF NOT EXISTS app_settings (
                            setting_key VARCHAR(100) PRIMARY KEY,
                            setting_value VARCHAR(500) NOT NULL
                        )
                    """);
                    log.info("Migration: table app_settings créée/vérifiée");
                } catch (SQLException e) {
                    log.warn("Migration app_settings: {}", e.getMessage());
                }

                // Migration 17 : vendor_id sur shop_orders (commandes liées directement au vendeur)
                try {
                    conn.createStatement().execute(
                        "ALTER TABLE shop_orders ADD COLUMN IF NOT EXISTS vendor_id BIGINT"
                    );
                    log.info("Migration: colonne vendor_id ajoutée sur shop_orders");
                } catch (SQLException e) {
                    log.warn("Migration shop_orders.vendor_id: {}", e.getMessage());
                }

                // Migration 18 : promo_label sur products (badge promo personnalisé PRO/PREMIUM)
                try {
                    conn.createStatement().execute(
                        "ALTER TABLE products ADD COLUMN IF NOT EXISTS promo_label VARCHAR(60)"
                    );
                    log.info("Migration: colonne promo_label ajoutée sur products");
                } catch (SQLException e) {
                    log.warn("Migration products.promo_label: {}", e.getMessage());
                }

                // Migration 19 : assigned_courier_id sur vendor_users (livreur assigné par admin)
                try {
                    conn.createStatement().execute(
                        "ALTER TABLE vendor_users ADD COLUMN IF NOT EXISTS assigned_courier_id BIGINT"
                    );
                    log.info("Migration: colonne assigned_courier_id ajoutée sur vendor_users");
                } catch (SQLException e) {
                    log.warn("Migration vendor_users.assigned_courier_id: {}", e.getMessage());
                }

                // Migration 20 : subscription_starts_at / subscription_expires_at → TIMESTAMP (LocalDateTime)
                // Convertit les colonnes DATE existantes en TIMESTAMP pour stocker l'heure exacte
                try {
                    conn.createStatement().execute(
                        "ALTER TABLE vendor_users ALTER COLUMN subscription_starts_at TYPE TIMESTAMP USING subscription_starts_at::TIMESTAMP"
                    );
                    log.info("Migration: subscription_starts_at converti en TIMESTAMP");
                } catch (SQLException e) {
                    log.warn("Migration subscription_starts_at TIMESTAMP: {}", e.getMessage());
                }
                try {
                    conn.createStatement().execute(
                        "ALTER TABLE vendor_users ALTER COLUMN subscription_expires_at TYPE TIMESTAMP USING subscription_expires_at::TIMESTAMP"
                    );
                    log.info("Migration: subscription_expires_at converti en TIMESTAMP");
                } catch (SQLException e) {
                    log.warn("Migration subscription_expires_at TIMESTAMP: {}", e.getMessage());
                }

                // Migration 21 : colonnes trend sur products
                try {
                    conn.createStatement().execute(
                        "ALTER TABLE products ADD COLUMN IF NOT EXISTS trend_active BOOLEAN DEFAULT FALSE"
                    );
                    log.info("Migration: colonne trend_active ajoutée sur products");
                } catch (SQLException e) {
                    log.warn("Migration products.trend_active: {}", e.getMessage());
                }
                try {
                    conn.createStatement().execute(
                        "ALTER TABLE products ADD COLUMN IF NOT EXISTS trend_expires_at TIMESTAMP"
                    );
                    log.info("Migration: colonne trend_expires_at ajoutée sur products");
                } catch (SQLException e) {
                    log.warn("Migration products.trend_expires_at: {}", e.getMessage());
                }

                // Migration 22 : paramètres boutique sur vendor_users
                String[][] shopParamCols = {
                    {"shop_status",   "VARCHAR(20) DEFAULT 'OPEN'"},
                    {"shop_language", "VARCHAR(50)"},
                    {"shop_hours",    "VARCHAR(200)"},
                };
                for (String[] col : shopParamCols) {
                    try {
                        conn.createStatement().execute(
                            "ALTER TABLE vendor_users ADD COLUMN IF NOT EXISTS " + col[0] + " " + col[1]
                        );
                        log.info("Migration: colonne {} ajoutée sur vendor_users", col[0]);
                    } catch (SQLException e) {
                        log.warn("Migration vendor_users.{}: {}", col[0], e.getMessage());
                    }
                }

                // Migration 23 : déduplication catégories (one-shot)
                // Garde la catégorie avec le plus petit ID pour chaque nom, redirige les produits
                try {
                    conn.createStatement().execute("""
                        UPDATE products SET category_id = (
                            SELECT MIN(id) FROM categories c2
                            WHERE LOWER(c2.name) = LOWER((SELECT name FROM categories c3 WHERE c3.id = products.category_id))
                        )
                        WHERE category_id IS NOT NULL
                    """);
                    log.info("Migration 23: références produits redirigées vers catégories canoniques");
                } catch (SQLException e) {
                    log.warn("Migration 23 update products: {}", e.getMessage());
                }
                try {
                    conn.createStatement().execute("""
                        DELETE FROM categories WHERE id NOT IN (
                            SELECT MIN(id) FROM categories GROUP BY LOWER(name)
                        )
                    """);
                    log.info("Migration 23: doublons catégories supprimés");
                } catch (SQLException e) {
                    log.warn("Migration 23 delete duplicates: {}", e.getMessage());
                }

                // Migration 24 : colonne display_name sur customers (nom d'affichage modifiable)
                try {
                    conn.createStatement().execute(
                        "ALTER TABLE customers ADD COLUMN IF NOT EXISTS display_name VARCHAR(60)"
                    );
                    log.info("Migration 24: colonne display_name ajoutée sur customers");
                } catch (SQLException e) {
                    log.warn("Migration 24 customers.display_name: {}", e.getMessage());
                }

                // Migration 25 : colonne delivery_mode sur vendor_users
                try {
                    conn.createStatement().execute(
                        "ALTER TABLE vendor_users ADD COLUMN IF NOT EXISTS delivery_mode VARCHAR(30) DEFAULT 'BOLA_COURIER'"
                    );
                    log.info("Migration 25: colonne delivery_mode ajoutée sur vendor_users");
                } catch (SQLException e) {
                    log.warn("Migration 25 vendor_users.delivery_mode: {}", e.getMessage());
                }

                // Migration 26 : colonne shop_country sur vendor_users
                try {
                    conn.createStatement().execute(
                        "ALTER TABLE vendor_users ADD COLUMN IF NOT EXISTS shop_country VARCHAR(2)"
                    );
                    log.info("Migration 26: colonne shop_country ajoutée sur vendor_users");
                } catch (SQLException e) {
                    log.warn("Migration 26 vendor_users.shop_country: {}", e.getMessage());
                }

                // Migration 27 : table product_comments
                try {
                    conn.createStatement().execute("""
                        CREATE TABLE IF NOT EXISTS product_comments (
                            id BIGSERIAL PRIMARY KEY,
                            product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                            parent_id BIGINT,
                            author_name VARCHAR(100),
                            customer_id BIGINT,
                            vendor_id BIGINT,
                            text VARCHAR(1000) NOT NULL,
                            likes_count INT NOT NULL DEFAULT 0,
                            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                            verified_buyer BOOLEAN NOT NULL DEFAULT FALSE,
                            deleted BOOLEAN NOT NULL DEFAULT FALSE
                        )
                    """);
                    conn.createStatement().execute(
                        "CREATE INDEX IF NOT EXISTS idx_product_comments_product_id ON product_comments(product_id)"
                    );
                    log.info("Migration 27: table product_comments créée");
                } catch (SQLException e) {
                    log.warn("Migration 27 product_comments: {}", e.getMessage());
                }

                // Migration 28 : colonnes tailles sur products
                try {
                    conn.createStatement().execute(
                        "ALTER TABLE products ADD COLUMN IF NOT EXISTS available_sizes VARCHAR(200)"
                    );
                    conn.createStatement().execute(
                        "ALTER TABLE products ADD COLUMN IF NOT EXISTS out_of_stock_sizes VARCHAR(200)"
                    );
                    log.info("Migration 28: colonnes available_sizes et out_of_stock_sizes ajoutées sur products");
                } catch (SQLException e) {
                    log.warn("Migration 28 products sizes: {}", e.getMessage());
                }

            } catch (Exception e) {
                log.error("Migration échouée: {}", e.getMessage());
            }
        };
    }
}
