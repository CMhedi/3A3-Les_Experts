-- EcoAdventure: Tables pour la gestion Packs & Inscriptions
-- (adaptées aux entités Entities.Pack et Entities.Inscription)

CREATE TABLE IF NOT EXISTS pack (
  id_pack INT AUTO_INCREMENT PRIMARY KEY,
  nom VARCHAR(120) NOT NULL,
  type_pack VARCHAR(40) NULL,
  prix_base DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  reduction DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  nb_activites_max INT NOT NULL DEFAULT 0,
  statut_pack VARCHAR(40) NULL
);

CREATE TABLE IF NOT EXISTS inscription (
  id_inscription INT AUTO_INCREMENT PRIMARY KEY,
  date_inscription DATETIME NOT NULL,
  statut_inscr VARCHAR(60) NOT NULL,
  montant_total DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  nom_user VARCHAR(255) DEFAULT NULL,
  nom_pack VARCHAR(255) DEFAULT NULL,
  id_user INT NOT NULL,
  id_pack INT NOT NULL,
  payment_gateway VARCHAR(50) DEFAULT NULL,
  payment_reference VARCHAR(100) DEFAULT NULL,
  payment_order_id VARCHAR(100) DEFAULT NULL,
  payment_status VARCHAR(50) DEFAULT NULL,
  paid_at DATETIME DEFAULT NULL,
  card_image VARCHAR(255) DEFAULT NULL,
  CONSTRAINT fk_inscription_pack FOREIGN KEY (id_pack) REFERENCES pack(id_pack)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  KEY idx_id_user (id_user),
  KEY idx_statut_inscr (statut_inscr),
  KEY idx_date_inscription (date_inscription)
);

-- Exemples (optionnel)
-- INSERT INTO pack (nom, type_pack, prix_base, reduction, nb_activites_max, statut_pack)
-- VALUES ('Premium', 'MENSUEL', 50.00, 0.00, 10, 'ACTIF');
