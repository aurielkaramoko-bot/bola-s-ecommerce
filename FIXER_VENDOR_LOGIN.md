# Correction: Problème de connexion des vendeurs approuvés

## 🐛 Problème Identifié
Les vendeurs approuvés par l'admin ne pouvaient pas se connecter avec leurs identifiants et mot de passe valides.

## 🔍 Cause Root
La vérification de connexion utilisait le **champ legacy `active`** (booléen) au lieu du vrai champ `vendorStatus` (ENUM).

### Ancien Code (INCORRECT)
```java
// VendorController.java - Ligne ~82-96
if (!v.isActive()) {  // ❌ Vérifie un champ legacy
    if (v.getVendorStatus() == VendorStatus.PENDING) {
        // message PENDING
    } else {
        // message SUSPENDED
    }
    return "redirect:/vendor/login";
}
```

**Problème:** 
- Quand l'admin approuvait un vendeur, il définissait `vendorStatus = ACTIVE`
- MAIS la vérification de connexion ignorait complètement ce champ
- Elle vérifiait seulement le champ `active` qui n'était pas synchronisé correctement

## ✅ Solution Mise en Œuvre

### 1. Correction du VendorController (Ligne 82-104)
La vérification utilise maintenant **exclusivement** `vendorStatus`:

```java
// Vérifier le statut du vendeur (non le champ legacy 'active')
if (v.getVendorStatus() == VendorStatus.PENDING) {
    ra.addFlashAttribute("flashError",
            "Votre demande d'ouverture de boutique est en cours de validation. Nous vous contacterons sous 24h.");
    return "redirect:/vendor/login";
} else if (v.getVendorStatus() == VendorStatus.SUSPENDED) {
    ra.addFlashAttribute("flashError",
            "Votre compte vendeur a été suspendu. Contactez l'administration.");
    return "redirect:/vendor/login";
}
// vendorStatus == ACTIVE → connexion autorisée
session.setAttribute(SESSION_KEY, v);
return "redirect:/vendor/dashboard";
```

### 2. Mise à jour du Template Admin (Ligne ~171-190)
Le template utilise maintenant `vendorStatus` au lieu de `!v.active`:

```html
<!-- Approuver si suspendu -->
<form th:if="${v.vendorStatus?.name() == 'SUSPENDED'}"
      th:action="@{/admin/vendors/{id}/approve(id=${v.id})}"
      method="post" class="m-0">
  <button type="submit" class="btn btn-sm btn-success">Activer</button>
</form>

<!-- Suspendre si actif -->
<form th:if="${v.vendorStatus?.name() == 'ACTIVE'}"
      th:action="@{/admin/vendors/{id}/suspend(id=${v.id})}"
      method="post" class="m-0">
  <button type="submit" class="btn btn-sm btn-outline-warning">Suspendre</button>
</form>
```

## 📋 Logique de Statut Vendeur

| Statut | Peut se connecter | Raison |
|--------|------------------|---------|
| PENDING | ❌ Non | En attente de validation admin |
| ACTIVE | ✅ Oui | Approuvé par l'admin |
| SUSPENDED | ❌ Non | Compte suspendu |

## 🧪 Impact
- ✅ Les vendeurs approuvés peuvent maintenant se connecter
- ✅ Les vendeurs en attente voient le message approprié
- ✅ Les vendeurs suspendus voient le message approprié
- ✅ Code plus cohérent et explicite

## 📝 Notes
Le champ `active` (booléen) reste dans le code AdminController pour la compatibilité legacy, mais n'est plus utilisé dans la logique de connexion critique.

## 🔧 Comment Tester
1. Créer un nouveau vendeur et l'approuver via `/admin/vendors`
2. Avec les identifiants du vendeur approuvé:
   - Identifiant ✓
   - Mot de passe ✓
   - Accès au dashboard `/vendor/dashboard` ✅
