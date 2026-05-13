-- ============================================================
-- VERIFICATION: Vérifier que la table inscription est correcte
-- ============================================================

-- 1. Afficher la structure complète de la table
DESCRIBE inscription;

-- 2. Vérifier l'AUTO_INCREMENT
SELECT TABLE_NAME, AUTO_INCREMENT FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_NAME = 'inscription' AND TABLE_SCHEMA = DATABASE();

-- 3. Compter les inscriptions existantes
SELECT COUNT(*) AS total_inscriptions FROM inscription;

-- 4. Afficher les 5 dernières inscriptions
SELECT id_inscription, date_inscription, statut_inscr, montant_total, nom_user, nom_pack
FROM inscription 
ORDER BY id_inscription DESC 
LIMIT 5;

-- 5. Vérifier que les colonnes de paiement existent
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'inscription' 
AND COLUMN_NAME IN ('payment_gateway', 'payment_reference', 'payment_order_id', 'payment_status', 'paid_at', 'card_image');

-- 6. Vérifier les index
SHOW INDEX FROM inscription;

-- 7. Vérifier les contraintes étrangères
SELECT CONSTRAINT_NAME, TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_NAME = 'inscription';

-- 8. Test d'insertion (pour vérifier que ça fonctionne)
-- Décommentez la ligne suivante pour tester une insertion
-- INSERT INTO inscription(date_inscription, statut_inscr, montant_total, nom_user, nom_pack, id_user, id_pack) VALUES (NOW(), 'EN_ATTENTE', 100.00, 'Test User', 'Test Pack', 1, 1);

-- 9. Si le test a fonctionné, afficher le nouvel ID
-- SELECT id_inscription FROM inscription WHERE nom_user = 'Test User' AND nom_pack = 'Test Pack' ORDER BY id_inscription DESC LIMIT 1;

-- 10. Nettoyer le test (décommentez si vous l'avez fait)
-- DELETE FROM inscription WHERE nom_user = 'Test User' AND nom_pack = 'Test Pack';
