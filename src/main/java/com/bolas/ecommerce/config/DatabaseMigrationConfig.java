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

            } catch (Exception e) {
                log.error("Migration échouée: {}", e.getMessage());
            }
        };
    }
}
