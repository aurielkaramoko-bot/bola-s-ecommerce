package com.bolas.ecommerce.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.*;

/**
 * Initialise Firebase Admin SDK.
 * - Production (Render) : lit /etc/secrets/firebase-service-account.json
 * - Développement local : lit classpath:bola-marketplace-firebase-adminsdk-fbs*.json
 * Si aucun fichier n'est trouvé, Firebase est désactivé silencieusement.
 */
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @PostConstruct
    public void init() {
        if (!FirebaseApp.getApps().isEmpty()) return; // déjà initialisé

        try {
            InputStream credStream = resolveCredentials();
            if (credStream == null) {
                log.warn("⚠️ Firebase désactivé — aucun fichier de credentials trouvé.");
                return;
            }
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credStream))
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("✅ Firebase Admin SDK initialisé.");
        } catch (Exception e) {
            log.warn("⚠️ Firebase non initialisé : {}", e.getMessage());
        }
    }

    private InputStream resolveCredentials() {
        // 1. Render production : fichier secret monté
        Path renderSecret = Path.of("/etc/secrets/firebase-service-account.json");
        if (Files.exists(renderSecret)) {
            try {
                log.info("Firebase: lecture depuis /etc/secrets/firebase-service-account.json");
                return Files.newInputStream(renderSecret);
            } catch (IOException e) {
                log.warn("Firebase: erreur lecture secret Render: {}", e.getMessage());
            }
        }

        // 2. Variable d'environnement (JSON inline)
        String envJson = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON");
        if (envJson != null && !envJson.isBlank()) {
            log.info("Firebase: lecture depuis variable d'environnement FIREBASE_SERVICE_ACCOUNT_JSON");
            return new ByteArrayInputStream(envJson.getBytes());
        }

        // 3. Développement local : classpath (nom exact ou avec suffixe variable)
        String[] candidateNames = {
            "bola-marketplace-firebase-adminsdk.json",
            "bola-marketplace-firebase-adminsdk-fbs.json",
            "bola-marketplace-firebase-adminsdk-fbsvc-540e64311f.json"
        };
        for (String name : candidateNames) {
            try {
                ClassPathResource res = new ClassPathResource(name);
                if (res.exists()) {
                    log.info("Firebase: lecture depuis classpath ({}) (dev local)", name);
                    return res.getInputStream();
                }
            } catch (IOException e) {
                log.debug("Firebase classpath {} non trouvé: {}", name, e.getMessage());
            }
        }
        // Dernier recours : scanner le classpath pour tout fichier adminsdk
        try {
            org.springframework.core.io.support.PathMatchingResourcePatternResolver resolver =
                new org.springframework.core.io.support.PathMatchingResourcePatternResolver();
            org.springframework.core.io.Resource[] resources =
                resolver.getResources("classpath:bola-marketplace-firebase-adminsdk*.json");
            if (resources.length > 0 && resources[0].exists()) {
                log.info("Firebase: lecture depuis classpath scan ({})", resources[0].getFilename());
                return resources[0].getInputStream();
            }
        } catch (IOException e) {
            log.debug("Firebase classpath scan échoué: {}", e.getMessage());
        }

        return null;
    }

    public static boolean isAvailable() {
        return !FirebaseApp.getApps().isEmpty();
    }
}
