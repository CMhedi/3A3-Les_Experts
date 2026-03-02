package enums;

public enum StatutReservation {
    CONFIRMEE("Ouvert"),
    ANNULEE("Annulé"),
    TERMINEE("Terminé");

    private final String label;

    StatutReservation(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
