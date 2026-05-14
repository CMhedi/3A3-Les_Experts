package enums;

public enum StatutReservation {
    EN_ATTENTE("En attente"),
    LISTE_ATTENTE("Liste d'attente"),
    CONFIRMEE("Confirmée"),
    ANNULEE("Annulée"),
    TERMINEE("Terminée"),
    /** Après scan / check-in ticket (pas de colonnes ticket en base). */
    SCANNEE("Scannée");

    private final String label;

    StatutReservation(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
