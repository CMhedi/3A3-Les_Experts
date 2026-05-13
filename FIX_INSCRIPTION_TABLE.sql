-- ============================================================
-- FIX: Corriger la table inscription pour l'AUTO_INCREMENT
-- ============================================================
-- Ce script corrige les problèmes de structure de la table inscription

-- Étape 1: Sauvegarder les données existantes
CREATE TABLE inscription_backup AS SELECT * FROM inscription;

-- Étape 2: Supprimer la table avec les contraintes étrangères
DROP TABLE IF EXISTS inscription;

-- Étape 3: Créer la table avec la structure correcte
CREATE TABLE `inscription` (
  `id_inscription` int(11) NOT NULL AUTO_INCREMENT,
  `date_inscription` datetime NOT NULL,
  `statut_inscr` varchar(255) NOT NULL,
  `montant_total` decimal(10,2) NOT NULL,
  `nom_user` varchar(255) DEFAULT NULL,
  `nom_pack` varchar(255) DEFAULT NULL,
  `id_user` int(11) DEFAULT NULL,
  `id_pack` int(11) DEFAULT NULL,
  `payment_gateway` varchar(50) DEFAULT NULL,
  `payment_reference` varchar(100) DEFAULT NULL,
  `payment_order_id` varchar(100) DEFAULT NULL,
  `payment_status` varchar(50) DEFAULT NULL,
  `paid_at` datetime DEFAULT NULL,
  `card_image` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_inscription`),
  KEY `idx_id_user` (`id_user`),
  KEY `idx_id_pack` (`id_pack`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Étape 4: Restaurer les données (optionnel, si vous avez des données importantes)
-- INSERT INTO inscription SELECT * FROM inscription_backup;
-- DROP TABLE inscription_backup;

-- Étape 5: Vérifier la structure
DESCRIBE inscription;

-- Étape 6: Afficher les informations AUTO_INCREMENT
SELECT TABLE_NAME, AUTO_INCREMENT FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_NAME = 'inscription';
