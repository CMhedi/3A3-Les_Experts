package enums;

public enum StatutReclamation {
    EN_ATTENTE, // Étape 1 : Soumise
    EN_COURS,   // Étape 2 : En cours de traitement
    TRAITEE,    // Étape 3 : Terminée
    REJETEE     // Étape 3 : Terminée (mais refusée)
}