package Models;

public class TopActivite {
    private final int idActivite;
    private final String nom;
    private final String typeActivite;
    private final String categorieAct;
    private final String niveauAct;
    private final int nbReservations;

    public TopActivite(int idActivite, String nom, String typeActivite,
                       String categorieAct, String niveauAct, int nbReservations) {
        this.idActivite = idActivite;
        this.nom = nom;
        this.typeActivite = typeActivite;
        this.categorieAct = categorieAct;
        this.niveauAct = niveauAct;
        this.nbReservations = nbReservations;
    }

    public int getIdActivite() { return idActivite; }
    public String getNom() { return nom; }
    public String getTypeActivite() { return typeActivite; }
    public String getCategorieAct() { return categorieAct; }
    public String getNiveauAct() { return niveauAct; }
    public int getNbReservations() { return nbReservations; }
}