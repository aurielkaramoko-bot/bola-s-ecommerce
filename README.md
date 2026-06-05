# 🛍️ Bola's — Boutique en ligne Spring Boot

> Plateforme e-commerce multi-vendeurs déployable sur Render, avec gestion des commandes, paiements par WhatsApp, fidélité client, et vérification d'identité par IA.

---

## 📋 Table des matières

- [Présentation](#-présentation)
- [Fonctionnalités](#-fonctionnalités)
- [Stack technique](#-stack-technique)
- [Architecture du projet](#-architecture-du-projet)
- [Plans vendeur](#-plans-vendeur)
- [Prérequis](#-prérequis)
- [Installation locale](#-installation-locale)
- [Variables d'environnement](#-variables-denvironnement)
- [Déploiement Docker](#-déploiement-docker)
- [Déploiement sur Render](#-déploiement-sur-render)
- [Accès et rôles](#-accès-et-rôles)
- [Structure des dossiers](#-structure-des-dossiers)

---

## 📖 Présentation

**Bola's** est une boutique en ligne multi-vendeurs construite avec **Spring Boot 3.3** et **Thymeleaf**. Elle permet à des vendeurs locaux (Côte d'Ivoire / Togo) de créer leur boutique en ligne, de gérer leur catalogue produits, de traiter les commandes et de communiquer avec leurs clients via WhatsApp.

La plateforme comprend trois espaces distincts :
- **Admin** — gestion globale de la plateforme
- **Vendeur** — gestion de sa boutique, produits, commandes, statistiques
- **Client** — navigation, panier, commandes, fidélité, tracking

---

## ✨ Fonctionnalités

### 🏪 Espace Vendeur
- Inscription avec vérification de pièce d'identité (Google Cloud Vision API)
- Gestion du catalogue produits (photos via Cloudinary)
- Tableau de bord avec statistiques (ventes, revenus, sessions)
- Chat en temps réel avec les clients
- Gestion des commandes (confirmer, préparer, expédier)
- Localisation GPS de la boutique (Google Maps)
- Sous-vendeurs (max 2 en PRO, 5 en PREMIUM)
- Réduction globale boutique avec date d'expiration
- Statut boutique : OUVERT / FERMÉ / VACANCES
- Code de parrainage unique avec bonus d'abonnement
- Bannière publicitaire sur la homepage (PREMIUM uniquement)

### 🛒 Espace Client
- Inscription / connexion Google OAuth2
- Panier persistant avec gestion des quantités
- Suivi de commande en temps réel
- Programme de fidélité (points, carte de fidélité)
- Avis et évaluations des produits
- Notifications (WhatsApp / Meta Cloud API)
- Chat avec le vendeur

### 🔧 Espace Admin
- Tableau de bord global (vendeurs, commandes, revenus)
- Approbation / suspension des vendeurs
- Gestion des catégories et sous-catégories
- Gestion des packs d'abonnement
- Audit log complet
- Rapports et signalements
- Gestion des livreurs

### 🔒 Sécurité
- Multi-authentification : Admin / Vendeur / Client avec `UserDetailsService` distincts
- Connexion sociale via Google OAuth2
- Protection contre les attaques par force brute (login attempt limiting)
- Rate limiting sur les endpoints sensibles
- Cookies sécurisés (HttpOnly, SameSite=Lax) en production
- Désactivation du cache sur les pages sensibles

---

## 🛠️ Stack technique

| Composant | Technologie |
|-----------|-------------|
| **Framework** | Spring Boot 3.3.5 |
| **Langage** | Java 17 |
| **Templating** | Thymeleaf + thymeleaf-extras-springsecurity6 |
| **Sécurité** | Spring Security + OAuth2 Client (Google) |
| **Persistance** | Spring Data JPA / Hibernate |
| **BDD Dev** | H2 (fichier local) |
| **BDD Prod** | PostgreSQL |
| **Images** | Cloudinary |
| **Vision IA** | Google Cloud Vision API (vérification identité) |
| **Notifications** | Meta WhatsApp Cloud API |
| **Cartographie** | Google Maps API |
| **Build** | Maven |
| **Déploiement** | Docker + Render |

---

## 🏗️ Architecture du projet

```
src/main/java/com/bolas/ecommerce/
├── BolasApplication.java          # Point d'entrée Spring Boot
├── api/                           # Endpoints REST publics (tracking, etc.)
├── config/
│   ├── ApplicationConfig.java     # Configuration générale
│   ├── CategorySeeder.java        # Données initiales des catégories
│   ├── DataInitializer.java       # Initialisation admin / données de base
│   └── DatabaseMigrationConfig.java # Migrations de schéma automatiques
├── controller/
│   ├── AdminController.java       # Espace administration
│   ├── VendorController.java      # Espace vendeur
│   ├── HomeController.java        # Page d'accueil et navigation publique
│   ├── CartController.java        # Gestion du panier
│   ├── CustomerAuthController.java# Authentification client
│   ├── ChatController.java        # Chat vendeur ↔ client
│   ├── ReviewReportController.java# Avis et signalements
│   └── ...
├── dto/                           # Data Transfer Objects
├── model/
│   ├── VendorUser.java            # Entité vendeur
│   ├── Product.java               # Entité produit
│   ├── CustomerOrder.java         # Entité commande
│   ├── Customer.java              # Entité client
│   ├── LoyaltyCard.java           # Carte de fidélité
│   └── ...
├── repository/                    # Interfaces Spring Data JPA
├── security/
│   ├── SecurityConfig.java        # Configuration Spring Security
│   ├── RateLimitingFilter.java    # Protection anti-flood
│   ├── LoginAttemptService.java   # Limitation des tentatives de connexion
│   └── ...
├── service/
│   ├── OrderFlowService.java      # Cycle de vie des commandes
│   ├── ImageUploadService.java    # Upload Cloudinary / local
│   ├── WhatsAppNotificationService.java
│   ├── CustomerLoyaltyService.java
│   ├── ReferralService.java       # Parrainage vendeurs
│   └── ...
└── util/                          # Utilitaires

src/main/resources/
├── application.properties         # Configuration développement (H2)
├── application-prod.properties    # Configuration production (PostgreSQL)
├── templates/                     # Templates Thymeleaf
│   ├── index.html                 # Page d'accueil
│   ├── admin/                     # Espace admin
│   ├── vendor/                    # Espace vendeur
│   ├── customer/                  # Espace client
│   └── fragments/                 # Composants partagés
└── static/                        # CSS, JS, images
```

---

## 💎 Plans Vendeur

| Plan | Prix | Produits | Commandes | Chat | Stats | Badge | Homepage |
|------|------|----------|-----------|------|-------|-------|----------|
| **GRATUIT** | 0 FCFA | 10 max | Via admin | ✗ | ✗ | ✗ | ✗ |
| **PRO LOCAL** | 5 000 FCFA/mois | Illimité | Autonome | ✓ | Basiques | ✗ | ✗ |
| **PRO** | 5 000 FCFA/mois | Illimité | Autonome | ✓ | Basiques | ✗ | ✗ |
| **PREMIUM** | 10 000 FCFA/mois | Illimité | Autonome | ✓ | Avancées | ⭐ Vérifié | Prioritaire |

---

## 🔧 Prérequis

- **Java 17+**
- **Maven 3.9+**
- **Git**
- (Optionnel) **Docker** pour le déploiement conteneurisé
- (Optionnel) **PostgreSQL** pour la production

---

## 🚀 Installation locale

### 1. Cloner le dépôt

```bash
git clone https://github.com/<votre-pseudo>/bolas-ecommerce.git
cd bolas-ecommerce
```

### 2. Lancer en mode développement (H2)

```bash
mvn spring-boot:run
```

L'application démarre sur **http://localhost:10000**

> La base de données H2 est stockée dans `./data/bolas.mv.db` et persistée entre les redémarrages (mode `update`).

### 3. Accès par défaut

| Rôle | URL | Identifiants par défaut |
|------|-----|------------------------|
| Admin | `/admin/dashboard` | `admin` / `admin123` |
| Vendeur | `/vendor/dashboard` | Créer un compte via `/vendor/register` |
| Client | `/` | Créer un compte ou Google OAuth2 |

> ⚠️ Changez les identifiants admin en production via les variables d'environnement `ADMIN_USERNAME` et `ADMIN_PASSWORD`.

---

## 🔑 Variables d'environnement

### Obligatoires en production

| Variable | Description |
|----------|-------------|
| `SPRING_DATASOURCE_URL` | URL JDBC PostgreSQL (ex: `jdbc:postgresql://host:5432/db`) |
| `SPRING_DATASOURCE_USERNAME` | Utilisateur base de données |
| `SPRING_DATASOURCE_PASSWORD` | Mot de passe base de données |
| `ADMIN_PASSWORD` | Mot de passe du compte administrateur |
| `GOOGLE_CLIENT_ID` | Client ID Google OAuth2 |
| `GOOGLE_CLIENT_SECRET` | Client Secret Google OAuth2 |

### Optionnelles / Fonctionnalités

| Variable | Description | Défaut |
|----------|-------------|--------|
| `ADMIN_USERNAME` | Nom d'utilisateur admin | `admin` |
| `CLOUDINARY_URL` | URL Cloudinary pour le stockage des images | *(local fallback)* |
| `GOOGLE_MAPS_API_KEY` | Clé API Google Maps | Clé de dev incluse |
| `META_WHATSAPP_TOKEN` | Token Meta WhatsApp Cloud API | *(désactivé)* |
| `META_WHATSAPP_PHONE_ID` | ID du numéro WhatsApp Business | *(désactivé)* |
| `WHATSAPP_ENABLED` | Activer/désactiver les notifications WA | `true` |
| `UPLOAD_PATH` | Répertoire de stockage des uploads | `uploads` |
| `PORT` | Port du serveur | `10000` |

---

## 🐳 Déploiement Docker

### Build de l'image

```bash
docker build -t bolas-ecommerce .
```

### Lancer le conteneur

```bash
docker run -p 10000:10000 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/bolas \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=secret \
  -e ADMIN_PASSWORD=MonMotDePasse! \
  -e GOOGLE_CLIENT_ID=xxx \
  -e GOOGLE_CLIENT_SECRET=yyy \
  -e CLOUDINARY_URL=cloudinary://... \
  bolas-ecommerce
```

Le Dockerfile utilise un **build multi-étapes** :
1. **Étape build** : Maven + JDK 17 → compile et package le JAR
2. **Étape runtime** : JRE 17 léger → exécute uniquement le JAR

---

## ☁️ Déploiement sur Render

1. Connecter le dépôt GitHub à [Render](https://render.com)
2. Créer un **Web Service** avec les paramètres :
   - **Runtime** : Docker
   - **Port** : `10000`
3. Créer une base de données **PostgreSQL** sur Render
4. Ajouter toutes les variables d'environnement listées ci-dessus dans l'onglet **Environment**
5. Déployer — Render détecte automatiquement le `Dockerfile`

> Le profil `prod` est activé automatiquement via l'`ENTRYPOINT` du Dockerfile : `-Dspring.profiles.active=prod`

---

## 👥 Accès et rôles

| Rôle | Portée | Authentification |
|------|--------|-----------------|
| `ADMIN` | Gestion complète de la plateforme | Formulaire (compte unique) |
| `VENDOR` | Sa boutique uniquement | Formulaire |
| `SELLER` | Sous-vendeur d'une boutique | Formulaire |
| `CUSTOMER` | Ses commandes et profil | Formulaire **ou** Google OAuth2 |

---

## 📁 Structure des dossiers racine

```
bolas-ecommerce/
├── src/                    # Code source Java + ressources
├── data/                   # Base de données H2 locale (gitignorée)
├── logs/                   # Journaux d'audit rotatifs
├── uploads/                # Images uploadées en local (dev)
├── pom.xml                 # Dépendances Maven
├── Dockerfile              # Image Docker multi-étapes
├── ACTION_PLAN.md          # Plan d'action du projet
├── CODE_SCAN_REPORT.md     # Rapport d'analyse du code
└── README.md               # Ce fichier
```

---

## 📄 Licence

Projet privé — © Bola's. Tous droits réservés.
