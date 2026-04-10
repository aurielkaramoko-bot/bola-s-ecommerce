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

            } catch (Exception e) {
                log.error("Migration échouée: {}", e.getMessage());
            }
        };
    }
}
