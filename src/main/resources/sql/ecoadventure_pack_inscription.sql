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
  id_user INT NOT NULL,
  id_pack INT NOT NULL,
  CONSTRAINT fk_inscription_pack FOREIGN KEY (id_pack) REFERENCES pack(id_pack)
    ON UPDATE CASCADE ON DELETE RESTRICT
);

-- Exemples (optionnel)
-- INSERT INTO pack (nom, type_pack, prix_base, reduction, nb_activites_max, statut_pack)
-- VALUES ('Premium', 'MENSUEL', 50.00, 0.00, 10, 'ACTIF');
