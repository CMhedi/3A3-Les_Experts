package Models;

public class CategoryAvailability {

    private String categorie;
    private int capaciteTotale;
    private int placesReservees;
    private int placesRestantes;
    private String statut;

    // ===== CONSTRUCTEUR =====
    public CategoryAvailability(String categorie,
                                int capaciteTotale,
                                int placesReservees,
                                int placesRestantes,
                                String statut) {

        this.categorie = categorie;
        this.capaciteTotale = capaciteTotale;
        this.placesReservees = placesReservees;
        this.placesRestantes = placesRestantes;
        this.statut = statut;
    }

    // ===== GETTERS (OBLIGATOIRES POUR JAVAFX) =====

    public String getCategorie() {
        return categorie;
    }

    public int getCapaciteTotale() {
        return capaciteTotale;
    }

    public int getPlacesReservees() {
        return placesReservees;
    }

    public int getPlacesRestantes() {
        return placesRestantes;
    }

    public String getStatut() {
        return statut;
    }

    // ===== SETTERS (optionnel mais conseillé) =====

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public void setCapaciteTotale(int capaciteTotale) {
        this.capaciteTotale = capaciteTotale;
    }

    public void setPlacesReservees(int placesReservees) {
        this.placesReservees = placesReservees;
    }

    public void setPlacesRestantes(int placesRestantes) {
        this.placesRestantes = placesRestantes;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }
}