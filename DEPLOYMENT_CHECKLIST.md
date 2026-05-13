# ✅ CHECKLIST DE DÉPLOIEMENT - Correction Inscription

## 🎯 OBJECTIF
Corriger le problème où les inscriptions ne sont pas sauvegardées en base de données.

---

## 📋 PRÉ-REQUIS

- [ ] Accès à la base de données MySQL
- [ ] Droits administrateur pour modifier les tables
- [ ] Maven installé localement
- [ ] Git installé (pour versionner les modifications)

---

## 🚀 ÉTAPES D'IMPLÉMENTATION

### PHASE 1: SAUVEGARDER LES DONNÉES

- [ ] Créer un backup de la base de données
  ```bash
  mysqldump -u root -p ecoadventure > backup_ecoadventure_$(date +%Y%m%d_%H%M%S).sql
  ```

- [ ] Vérifier que le backup a été créé
  ```bash
  ls -lh backup_ecoadventure_*.sql
  ```

---

### PHASE 2: EXÉCUTER LES CORRECTIONS SQL

**Option A: Via phpMyAdmin (Recommandé pour débutants)**
- [ ] Ouvrir phpMyAdmin
- [ ] Sélectionner la base de données `ecoadventure`
- [ ] Ouvrir l'onglet "SQL"
- [ ] Importer/Copier le contenu de `FIX_INSCRIPTION_TABLE.sql`
- [ ] Cliquer "Exécuter"
- [ ] Vérifier qu'aucune erreur n'apparaît

**Option B: Via ligne de commande (Pour développeurs)**
- [ ] Ouvrir le terminal
- [ ] Naviguer vers le dossier du projet
  ```bash
  cd c:\Users\windows\ 10\Desktop\web\Esprit-PIDEV-JAVA-3A3-EcoAdventure
  ```
- [ ] Exécuter le script SQL
  ```bash
  mysql -u root -p ecoadventure < FIX_INSCRIPTION_TABLE.sql
  ```

---

### PHASE 3: VÉRIFIER LA BASE DE DONNÉES

- [ ] Exécuter les vérifications
  ```bash
  mysql -u root -p ecoadventure < VERIFY_INSCRIPTION_TABLE.sql
  ```

- [ ] Vérifier que la table a `AUTO_INCREMENT`
  - [ ] La colonne `id_inscription` doit avoir "auto_increment"
  - [ ] Les colonnes de paiement doivent être présentes

---

### PHASE 4: METTRE À JOUR LE CODE

Les fichiers suivants ont déjà été corrigés. Vérifiez-les:

- [ ] `src/main/resources/sql/ecoadventure_pack_inscription.sql`
  - [ ] ✅ Contient AUTO_INCREMENT
  - [ ] ✅ Inclut toutes les colonnes de paiement

- [ ] `src/main/java/Services/InscriptionService.java`
  - [ ] ✅ Méthode `add()` retourne `int` (l'ID généré)
  - [ ] ✅ Utilise `Statement.RETURN_GENERATED_KEYS`

- [ ] `src/main/java/Services/PackInscriptionService.java`
  - [ ] ✅ Utilise `inscriptionService.add()`
  - [ ] ✅ Capture et définit l'ID généré

- [ ] `src/main/java/controllers/PackInscriptionViewController.java`
  - [ ] ✅ Capture l'ID retourné
  - [ ] ✅ Affiche l'ID dans le message de confirmation

- [ ] `src/main/java/controllers/InscriptionFormController.java`
  - [ ] ✅ Capture l'ID retourné pour les nouvelles inscriptions

---

### PHASE 5: COMPILER ET TESTER

- [ ] Nettoyer et recompiler
  ```bash
  mvn clean compile
  ```
  - [ ] ✅ Aucune erreur de compilation

- [ ] Exécuter les tests
  ```bash
  mvn test
  ```
  - [ ] ✅ Tous les tests passent

- [ ] Exécuter les tests d'inscription spécifiques (si créé)
  ```bash
  mvn test -Dtest=InscriptionServiceFixTest
  ```
  - [ ] ✅ Test 1 PASSED
  - [ ] ✅ Test 2 PASSED
  - [ ] ✅ Test 3 PASSED
  - [ ] ✅ Test 4 PASSED

---

### PHASE 6: DÉPLOYER ET TESTER L'APPLICATION

- [ ] Lancer l'application
  ```bash
  mvn spring-boot:run
  ```
  (ou votre commande de lancement habituelle)

- [ ] Naviguer vers "Pack Inscription"

- [ ] Test 1: Créer une nouvelle inscription
  - [ ] Remplir le formulaire avec les données de test
  - [ ] Cliquer "Confirmer l'inscription"
  - [ ] ✅ Message de confirmation affiche l'ID (ex: "ID Inscription : #123")
  - [ ] ✅ Message affiche le montant correct

- [ ] Test 2: Vérifier en base de données
  ```sql
  SELECT * FROM inscription WHERE nom_user = 'Votre Nom' ORDER BY id_inscription DESC LIMIT 1;
  ```
  - [ ] ✅ L'inscription apparaît en base de données
  - [ ] ✅ L'ID correspond à celui affiché
  - [ ] ✅ Le montant est correct
  - [ ] ✅ La date/heure est correcte

- [ ] Test 3: Créer plusieurs inscriptions
  - [ ] Créer 3-5 nouvelles inscriptions
  - [ ] ✅ Chaque inscription a un ID unique
  - [ ] ✅ Les IDs augmentent
  - [ ] ✅ Toutes les inscriptions sont sauvegardées

---

## 🔍 VÉRIFICATION FINALE

- [ ] Les inscriptions s'affichent dans le formulaire "Inscription List"
  ```sql
  SELECT COUNT(*) AS total FROM inscription;
  ```
  - [ ] ✅ Le nombre d'inscriptions augmente

- [ ] Les paiements futurs peuvent être associés
  ```sql
  SELECT id_inscription, payment_status, payment_gateway FROM inscription WHERE payment_status IS NOT NULL;
  ```
  - [ ] ✅ Les colonnes de paiement existent et sont prêtes

- [ ] Les index fonctionnent correctement
  ```sql
  SHOW INDEX FROM inscription;
  ```
  - [ ] ✅ Les index sont présents

---

## 📊 RÉSUMÉ DES CHANGEMENTS

| Composant | Avant | Après |
|-----------|-------|-------|
| **Table SQL** | Pas d'AUTO_INCREMENT | ✅ AUTO_INCREMENT |
| **Colonnes** | Manquant de colonnes | ✅ Complètes (paiement) |
| **Méthode add()** | `void` | ✅ Retourne `int` |
| **Gestion ID** | ❌ Pas de retour | ✅ Retourne ID généré |
| **Sauvegarde** | ❌ Échoue | ✅ Fonctionne |
| **Affichage** | Pas d'ID | ✅ Affiche l'ID |

---

## ⚠️ POINT CRITIQUE

⚠️ **LA TABLE DOIT AVOIR AUTO_INCREMENT!**

Si vous voyez cette erreur:
```
Field 'id_inscription' doesn't have a default value
```

Cela signifie que le script SQL n'a pas été correctement exécuté. Re-exécutez `FIX_INSCRIPTION_TABLE.sql`.

---

## 🆘 EN CAS DE PROBLÈME

### Les inscriptions ne sont toujours pas sauvegardées
1. Vérifier que `MyDB2.getConnection()` fonctionne
2. Vérifier les logs de l'application pour les exceptions
3. Vérifier que les données de test sont valides (id_user et id_pack existent)

### Erreur: "Cannot add or update a child row"
→ L'id_pack n'existe pas dans la table pack
→ Assurez-vous qu'un pack avec l'ID 1 existe:
```sql
SELECT * FROM pack WHERE id_pack = 1;
```

### L'ID n'est pas affiché
→ Vérifier que le contrôleur capture bien l'ID
→ Vérifier les logs "✅ Inscription ID généré"

---

## 📝 DOCUMENTS DE RÉFÉRENCE

- `INSCRIPTION_FIX_SUMMARY.md` - Détails complets des corrections
- `QUICK_FIX_GUIDE.md` - Guide rapide
- `VERIFY_INSCRIPTION_TABLE.sql` - Script de vérification
- `FIX_INSCRIPTION_TABLE.sql` - Script de correction
- `src/test/java/Services/InscriptionServiceFixTest.java` - Tests unitaires

---

## ✅ SIGNATURE D'APPROBATION

- [ ] Corrections testées localement: _______________
- [ ] Tests en base de données réussis: _______________
- [ ] Application testée: _______________
- [ ] Prêt pour déploiement: _______________

---

**Date**: 13 Mai 2026
**Statut**: 🟢 PRÊT POUR DÉPLOIEMENT
**Versions affectées**: 3.0+
