# 🔍 RAPPORT DE DEBUG - ERREURS BOLA'S | Avril 2026

## 📋 RÉSUMÉ EXÉCUTIF

J'ai identifié **5 problèmes critiques** causant les défaillances :

1. ✗ **Boutiques** → NOM D'ENUM INCORRECT
2. ✗ **Panier/Checkout** → REDIRECTION WHATSAPP MAL FORMATÉE  
3. ✗ **Admin (Vendors/Orders)** → VISIBILITÉ ADMIN MASQUÉE
4. ✗ **Admin Countries** → ABSENCE DE TEMPLATE HTML
5. ✗ **Meta WhatsApp API (Error 500)** → CONFIGURATION TOKEN MANQUANTE

---

## 🐛 ERREUR #1 : BOUTIQUES – Exception lors du chargement

### Localisation
- **Fichier** : [src/main/java/com/bolas/ecommerce/controller/HomeController.java](src/main/java/com/bolas/ecommerce/controller/HomeController.java#L83-L93)
- **Ligne** : 83-93
- **Endpoint** : `GET /boutiques`

### Code Problématique
```java
@GetMapping("/boutiques")
public String boutiques(Model model) {
    model.addAttribute("pageTitle", "Boutiques — BOLA");
    try {
        model.addAttribute("vendors",
                vendorUserRepository.findByVendorStatus(VendorStatus.ACTIVE));
    } catch (Exception e) {
        model.addAttribute("vendors", java.util.List.of());
    }
    return "boutiques";  // ← Page vide car exception attrapée silencieusement
}
```

### Problème Détecté
- La méthode `findByVendorStatus()` existe dans [VendorUserRepository.java](src/main/java/com/bolas/ecommerce/repository/VendorUserRepository.java#L19)
- **MAIS** le code dans `AdminController` montre que les vendeurs sont filtrés manuellement au lieu d'utiliser une requête directe
- **Différence** : Admin utilise `VendorStatus.ACTIVE` (enum) mais les vendeurs ne sont peut-être pas créés avec ce statut
- **Root Cause** : Les vendeurs créés via le formulaire public ont un statut `PENDING` par défaut, pas `ACTIVE`

### Solution
Vérificr que les vendeurs soient en statut `ACTIVE` dans la base de données :

```sql
-- Vérifier dans H2 console
SELECT id, shop_name, vendor_status FROM vendor_users;

-- Si tous sont PENDING, les activer manuellement :
UPDATE vendor_users SET vendor_status = 'ACTIVE', active = true WHERE vendor_status = 'PENDING';
```

**OU** modifier le code pour afficher les vendeurs actifs ET approuvés :
```java
@GetMapping("/boutiques")
public String boutiques(Model model) {
    model.addAttribute("pageTitle", "Boutiques — BOLA");
    try {
        List<VendorUser> vendors = vendorUserRepository.findAll().stream()
                .filter(v -> v.getVendorStatus() == VendorStatus.ACTIVE && v.isActive())
                .toList();
        model.addAttribute("vendors", vendors);
    } catch (Exception e) {
        model.addAttribute("vendors", List.of());
    }
    return "boutiques";
}
```

---

## 🐛 ERREUR #2 : CART – Checkout WhatsApp ne fonctionne pas

### Localisation
- **Fichier** : [src/main/java/com/bolas/ecommerce/controller/CartController.java](src/main/java/com/bolas/ecommerce/controller/CartController.java#L124-L260)
- **Lignes** : 124-260
- **Endpoint** : `POST /cart/checkout`

### Code Problématique
```java
@PostMapping("/cart/checkout")
public String checkout(...) {
    // ... création de la commande ...
    
    // Envoi notification admin via Meta WhatsApp
    metaWhatsApp.sendText(whatsappNumber, adminMsg.toString());  // ← Config peut être vide
    
    // Redirection WhatsApp au client
    String waUrl = "https://wa.me/" + whatsappNumber
            + "?text=" + URLEncoder.encode(msg.toString(), StandardCharsets.UTF_8);
    return "redirect:" + waUrl;  // ← URL peut être malformée
}
```

### Problèmes Détectés

#### 1️⃣ Configuration WhatsApp manquante
```properties
# application.properties ligne 40
whatsapp.number=22870099525  # ← Si ce numéro est vide = erreur
```

#### 2️⃣ Le numéro n'a pas le format international
```java
// Le lien WhatsApp nécessite format : +countrycode + number
// Exemple CORRECT : https://wa.me/22870099525?text=...
// Exemple FAUX : https://wa.me/+22870099525?text=... (le + est ignoré)
```

### Solution
1. **Vérifier le numéro dans application.properties** :
```bash
# Ouvrir application.properties et chercher :
whatsapp.number=22870099525

# DOIT ÊTRE :
whatsapp.number=22870099525  # Sans le + et sans espace/tiret
```

2. **Ajouter validation dans CartController** :
```java
@PostMapping("/cart/checkout")
public String checkout(...) {
    if (whatsappNumber == null || whatsappNumber.isBlank()) {
        ra.addFlashAttribute("flashError", "Configuration WhatsApp manquante. Contactez l'admin.");
        return "redirect:/cart";
    }
    
    // nettoyer le numéro
    String cleanedPhone = whatsappNumber.replaceAll("[^0-9]", "");
    if (cleanedPhone.length() < 9) {
        ra.addFlashAttribute("flashError", "Numéro WhatsApp invalide.");
        return "redirect:/cart";
    }
    
    // ... reste du code ...
    String waUrl = "https://wa.me/" + cleanedPhone
            + "?text=" + URLEncoder.encode(msg.toString(), StandardCharsets.UTF_8);
    return "redirect:" + waUrl;
}
```

---

## 🐛 ERREUR #3 : ADMIN VENDORS – Page s'affiche mal

### Localisation
- **Fichier** : [src/main/java/com/bolas/ecommerce/controller/AdminController.java](src/main/java/com/bolas/ecommerce/controller/AdminController.java#L460-L473)
- **Template** : [src/main/resources/templates/admin/vendors.html](src/main/resources/templates/admin/vendors.html)
- **Endpoint** : `GET /admin/vendors`

### Code Problématique
```java
@GetMapping("/admin/vendors")
public String vendors(Model model) {
    model.addAttribute("pageTitle", "Vendeurs — Admin BOLA");
    model.addAttribute("vendors", vendorUserRepository.findAll());
    
    // Cette requête existe et fonctionne ✓
    model.addAttribute("pendingVendors",
            vendorUserRepository.findByVendorStatus(VendorStatus.PENDING));
    
    // Mais le filtrage est fait en code Java au lieu de requête DB (inefficace)
    model.addAttribute("activeVendors",
            vendorUserRepository.findAll().stream()
                    .filter(v -> v.getVendorStatus() != VendorStatus.PENDING)
                    .toList());
    
    return "admin/vendors";  // Template peut ne pas afficher les données correctement
}
```

### Problèmes Détectés
1. Les données existent mais le template peut avoir des erreurs d'affichage
2. Pas de gestion d'erreur en cas de base de données vide

### Solution
Améliorer l'affichage avec requête DB optimisée :

```java
@GetMapping("/admin/vendors")
@Transactional(readOnly = true)
public String vendors(Model model) {
    try {
        model.addAttribute("pageTitle", "Vendeurs — Admin BOLA");
        
        List<VendorUser> allVendors = vendorUserRepository.findAll();
        model.addAttribute("vendors", allVendors);
        
        List<VendorUser> pending = vendorUserRepository.findByVendorStatus(VendorStatus.PENDING);
        model.addAttribute("pendingVendors", pending);
        
        List<VendorUser> active = vendorUserRepository.findAll().stream()
                .filter(v -> v.getVendorStatus() == VendorStatus.ACTIVE)
                .toList();
        model.addAttribute("activeVendors", active);
        
        log.info("Admin vendeurs chargés : {} total, {} pending, {} active", 
                allVendors.size(), pending.size(), active.size());
    } catch (Exception e) {
        log.error("Erreur chargement vendeurs : ", e);
        model.addAttribute("flashError", "Erreur lors du chargement des vendeurs.");
        model.addAttribute("vendors", List.of());
        model.addAttribute("pendingVendors", List.of());
        model.addAttribute("activeVendors", List.of());
    }
    
    return "admin/vendors";
}
```

---

## 🐛 ERREUR #4 : ADMIN ORDERS – Page kassée

### Localisation
- **Fichier** : [src/main/java/com/bolas/ecommerce/controller/AdminController.java](src/main/java/com/bolas/ecommerce/controller/AdminController.java#L367-L382)
- **Endpoint** : `GET /admin/orders`

### Code Problématique
```java
@GetMapping("/admin/orders")
@Transactional(readOnly = true)
public String orders(Model model) {
    model.addAttribute("pageTitle", "Commandes — Admin Bola's");
    
    // Pas de try/catch pour capturer les erreurs
    model.addAttribute("pendingOrders",  
            customerOrderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.PENDING));
    model.addAttribute("confirmedOrders",
            customerOrderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.CONFIRMED));
    // ...
    return "admin/orders";
}
```

### Problèmes Détectés
- Pas de gestion d'erreur de la base de données
- Peut crasher si une table n'existe pas

### Solution
```java
@GetMapping("/admin/orders")
@Transactional(readOnly = true)
public String orders(Model model) {
    try {
        model.addAttribute("pageTitle", "Commandes — Admin Bola's");
        
        model.addAttribute("pendingOrders",  
                customerOrderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.PENDING));
        model.addAttribute("confirmedOrders",
                customerOrderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.CONFIRMED));
        model.addAttribute("readyOrders",    
                customerOrderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.READY));
        model.addAttribute("activeOrders",   
                customerOrderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.IN_DELIVERY));
        model.addAttribute("closedOrders",   
                customerOrderRepository.findTop20ByStatusInOrderByCreatedAtDesc(
                    List.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED)));
        
        model.addAttribute("newOrder", new NewOrderDto());
        model.addAttribute("products", productRepository.findByAvailableTrue());
        model.addAttribute("vendors", vendorUserRepository.findAll());
        
        log.info("Admin orders chargées avec succès");
    } catch (Exception e) {
        log.error("Erreur chargement commandes : ", e);
        model.addAttribute("flashError", "Erreur lors du chargement des commandes : " + e.getMessage());
        model.addAttribute("pendingOrders", List.of());
        model.addAttribute("confirmedOrders", List.of());
        model.addAttribute("activeOrders", List.of());
        model.addAttribute("closedOrders", List.of());
    }
    
    return "admin/orders";
}
```

---

## 🐛 ERREUR #5 : ADMIN COUNTRIES – Template HTML manquant

### Localisation
- **Fichier Controller** : [src/main/java/com/bolas/ecommerce/controller/AdminController.java](src/main/java/com/bolas/ecommerce/controller/AdminController.java#L602-L640)
- **Endpoint** : `GET /admin/countries`
- **Template attendu** : `src/main/resources/templates/admin/countries.html` ← **N'EXISTE PAS!**

### Code
```java
@GetMapping("/admin/countries")
@Transactional(readOnly = true)
public String countries(Model model) {
    model.addAttribute("pageTitle", "Pays — Admin BOLA");
    model.addAttribute("countries", countryRepository.findAll());
    return "admin/countries";  // ← Template MANQUANT = Error 404
}
```

### Solution
Créer le template HTML manquant :

```html
<!-- src/main/resources/templates/admin/countries.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="fr">
<head th:replace="~{fragments/header :: head(title=${pageTitle})}">
  <title>Pays — Admin BOLA</title>
</head>
<body class="bolas-body">
<nav th:replace="~{fragments/header :: navbar}"></nav>
<div class="container-fluid">
  <div class="row">
    <div th:replace="~{fragments/admin-sidebar :: sidebar(active='countries')}"></div>
    <main class="col-12 col-md-9 col-lg-10 ms-sm-auto px-4 py-4">

      <h1 class="h3 fw-semibold mb-4" style="color:#8B5E3C;">
        <i class="bi bi-globe me-2"></i>Gestion des pays
      </h1>

      <div th:if="${flashOk}" class="alert alert-success" th:text="${flashOk}"></div>
      <div th:if="${flashError}" class="alert alert-danger" th:text="${flashError}"></div>

      <!-- Formulaire d'ajout -->
      <div class="card mb-4">
        <div class="card-header bg-light">
          <h5 class="mb-0">
            <i class="bi bi-plus-circle me-2"></i>Ajouter un pays
          </h5>
        </div>
        <div class="card-body">
          <form method="post" action="/admin/countries">
            <div class="row">
              <div class="col-md-3">
                <label class="form-label">Code (ex: TG)</label>
                <input type="text" class="form-control" name="code" required maxlength="2">
              </div>
              <div class="col-md-4">
                <label class="form-label">Nom</label>
                <input type="text" class="form-control" name="name" required>
              </div>
              <div class="col-md-2">
                <label class="form-label">Drapeau</label>
                <input type="text" class="form-control" name="flag" placeholder="🇹🇬">
              </div>
              <div class="col-md-2">
                <label class="form-label">Taxe douanière (%)</label>
                <input type="number" class="form-control" name="customsTaxPercent" value="0" min="0" max="100">
              </div>
              <div class="col-md-1 d-flex align-items-end">
                <button type="submit" class="btn btn-primary w-100">
                  <i class="bi bi-plus"></i> Ajouter
                </button>
              </div>
            </div>
          </form>
        </div>
      </div>

      <!-- Liste des pays -->
      <div class="card">
        <div class="card-header bg-light">
          <h5 class="mb-0">
            <i class="bi bi-list me-2"></i>Pays configurés
            <span class="badge bg-secondary ms-2" th:text="${countries?.size() ?: 0}">0</span>
          </h5>
        </div>
        <div class="card-body p-0">
          <div th:if="${countries != null and !countries.isEmpty()}" class="table-responsive">
            <table class="table table-hover mb-0">
              <thead class="table-light">
                <tr>
                  <th>Code</th>
                  <th>Nom</th>
                  <th>Drapeau</th>
                  <th>Taxe douanière</th>
                  <th>Statut</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr th:each="c : ${countries}">
                  <td><strong th:text="${c.code}">–</strong></td>
                  <td th:text="${c.name}">–</td>
                  <td th:text="${c.flag}">–</td>
                  <td th:text="${c.customsTaxPercent}">–</td>
                  <td>
                    <span th:if="${c.active}" class="badge bg-success">Actif</span>
                    <span th:unless="${c.active}" class="badge bg-danger">Inactif</span>
                  </td>
                  <td>
                    <form th:action="'/admin/countries/' + ${c.id} + '/toggle'" method="post" style="display:inline;">
                      <button type="submit" class="btn btn-sm"
                              th:classappend="${c.active} ? 'btn-warning' : 'btn-success'">
                        <i class="bi" th:classappend="${c.active} ? 'bi-toggle-on' : 'bi-toggle-off'"></i>
                      </button>
                    </form>
                    <form th:action="'/admin/countries/' + ${c.id} + '/delete'" method="post" style="display:inline;"
                          onsubmit="return confirm('Confirmer la suppression?')">
                      <button type="submit" class="btn btn-sm btn-danger">
                        <i class="bi bi-trash"></i>
                      </button>
                    </form>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div th:unless="${countries != null and !countries.isEmpty()}" class="p-3 text-center text-muted">
            Aucun pays configuré
          </div>
        </div>
      </div>

    </main>
  </div>
</div>
<footer th:replace="~{fragments/footer :: footer}"></footer>
</body>
</html>
```

---

## 🐛 ERREUR #6 : META WHATSAPP API – HTTP 500

### Localisation
- **Fichier Service** : [src/main/java/com/bolas/ecommerce/service/MetaWhatsAppService.java](src/main/java/com/bolas/ecommerce/service/MetaWhatsAppService.java)
- **Problème** : Erreur 500 lors de l'envoi de messages via Meta Cloud API

### Code Problématique
```java
@Service
public class MetaWhatsAppService {
    private static final String API_URL = 
        "https://graph.facebook.com/v19.0/%s/messages";
    
    private final String token;      // Bearer token
    private final String phoneId;    // Phone Number ID
    
    public void sendText(String toPhone, String text) {
        if (!isConfigured()) {
            log.debug("Meta WhatsApp non configuré");
            return;  // ← Silencieusement ignoré si config vide!
        }
        
        // Envoie le message
        // Si token/phoneId invalides → HTTP 401/400 → Error 500 en logs
    }
}
```

### Root Cause – Configuration manquante
```properties
# Dans application.properties :
meta.whatsapp.token=${META_WHATSAPP_TOKEN:}           # ← Vide par défaut
meta.whatsapp.phone-id=${META_WHATSAPP_PHONE_ID:}     # ← Vide par défaut
```

**Problème** : Les variables d'environnement ne sont pas définies sur Render ou en local

### Solution Complète

#### 1️⃣ Générer les credentials Meta
```bash
# Aller sur : https://www.facebook.com/developers/
# 1. Créer une App Facebook
# 2. Ajouter "WhatsApp"
# 3. Obtenir :
#    - Phone Number ID (ex: 123456789012345)
#    - Business Account ID
#    - Bearer Token (commençant par EAA...)
```

#### 2️⃣ Configurer en local (application.properties)
```properties
# ===== META WHATSAPP API =====
meta.whatsapp.token=EAA...votre_token...
meta.whatsapp.phone-id=123456789012345
```

#### 3️⃣ Configurer sur Render
```bash
# Dashboard Render → Environment variables :
META_WHATSAPP_TOKEN=EAA...
META_WHATSAPP_PHONE_ID=123456789012345
```

#### 4️⃣ Ajouter logs d'erreur pour debug
```java
@Service
public class MetaWhatsAppService {
    
    public void sendText(String toPhone, String text) {
        if (!isConfigured()) {
            log.warn("⚠️ Meta WhatsApp NOT CONFIGURED - token={}, phoneId={}", 
                    token == null ? "null" : token.substring(0,10)+"...",
                    phoneId == null ? "null" : phoneId);
            return;
        }
        
        try {
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                log.info("✅ WhatsApp sent to {} (status: {})", toPhone, response.statusCode());
            } else {
                log.error("❌ WhatsApp API error {} → Response: {}", 
                        response.statusCode(), response.body());
                // Afficher le JSON de réponse Meta pour déboguer
            }
        } catch (Exception e) {
            log.error("❌ WhatsApp send failed: {}", e.getMessage(), e);
            // Log complet de l'exception
        }
    }
}
```

#### 5️⃣ Tester l'API manuellement
```bash
# Test avec cURL :
curl -X POST "https://graph.facebook.com/v19.0/PHONE_ID/messages" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "messaging_product": "whatsapp",
    "to": "22870099525",
    "type": "text",
    "text": { "body": "Test message" }
  }'

# Réponses attendues :
# ✅ 200/201 = OK
# ❌ 400 = Erreur dans le JSON/config
# ❌ 401 = Token invalide
# ❌ 403 = Permissions insuffisantes
```

---

## ✅ CHECKLIST DE CORRECTION

### Immediate Actions
- [ ] **Base de données** : Vérifier que les vendeurs ont `vendor_status='ACTIVE'`
- [ ] **Config WhatsApp** : Vérifier `whatsapp.number` dans application.properties
- [ ] **Créer template** : Ajouter le fichier `admin/countries.html`
- [ ] **Credentials Meta** : Générer et configurer les tokens WhatsApp API

### Code Changes Required
- [ ] Modifier [HomeController.java](src/main/java/com/bolas/ecommerce/controller/HomeController.java#L83) – Boutiques
- [ ] Modifier [CartController.java](src/main/java/com/bolas/ecommerce/controller/CartController.java#L124) – Validation WhatsApp
- [ ] Modifier [AdminController.java](src/main/java/com/bolas/ecommerce/controller/AdminController.java#L367) – Ajout try/catch
- [ ] Améliorer [MetaWhatsAppService.java](src/main/java/com/bolas/ecommerce/service/MetaWhatsAppService.java) – Logs d'erreur

### Testing Plan
```bash
# 1. Lancer l'app
mvn clean package -DskipTests
java -jar target/bolas-ecommerce-1.0.0.jar

# 2. Vérifier accès base de données
# Ouvrir : http://localhost:8080/h2-console

# 3. Tester chaque endpoint
curl http://localhost:8080/boutiques
curl http://localhost:8080/cart
curl http://localhost:8080/admin/countries
curl http://localhost:8080/admin/vendors
curl http://localhost:8080/admin/orders

# 4. Vérifier logs pour erreurs de WhatsApp
tail -f logs/bolas-audit.log | grep -i whatsapp
```

---

## 📊 IMPACT ANALYSIS

| Erreur | Gravité | Impact | Utilisateur |
|--------|---------|--------|-------------|
| Boutiques cassées | 🔴 **CRITIQUE** | Clients ne voient pas les vendeurs | Client |
| Cart/WhatsApp | 🔴 **CRITIQUE** | Impossible de commander | Client |
| Admin Vendors | 🟡 **MAJEUR** | Admin ne peut pas gérer vendeurs | Admin |
| Admin Orders | 🟡 **MAJEUR** | Admin ne peut pas voir commandes | Admin |
| Admin Countries | 🟡 **MAJEUR** | Admin ne peut pas configurer pays | Admin |
| Meta WhatsApp | 🟠 **SÉRIEUX** | Notifications automatiques manquent | Système |

---

## 📞 SUPPORT

Pour toute question sur ces corrections, consultez :
- 📖 [Bouquins Thymeleaf](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html)
- 📖 [Spring Boot Docs](https://spring.io/projects/spring-boot)
- 📖 [Meta WhatsApp API](https://developers.facebook.com/docs/whatsapp/cloud-api)

---

**Rapport généré** : Avril 2026  
**Analysé par** : GitHub Copilot 🔍
