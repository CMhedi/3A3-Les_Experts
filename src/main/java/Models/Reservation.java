package Models;

import java.time.LocalDateTime;

/**
 * Modèle aligné sur {@code reservation_activite} (ecoadventure-3.sql).
 * Colonnes : id_res_act, statut_res, nb_personnes, id_user, id_activite, date_reservation, ville_user.
 */
public class Reservation {

    private int id;
    private LocalDateTime dateReservation;
    private String statut;
    private int nbPersonnes;
    private int idUser;
    private int idActivite;
    private String villeUser;

    /** Champs d’affichage (requête avec JOIN), non persistés par ce modèle seul. */
    private String activiteNom;
    private String userDisplay;

    public Reservation() {}

    public Reservation(int id, LocalDateTime dateReservation, String statut, int nbPersonnes,
                       int idUser, int idActivite, String villeUser) {
        this.id = id;
        this.dateReservation = dateReservation;
        this.statut = statut;
        this.nbPersonnes = nbPersonnes;
        this.idUser = idUser;
        this.idActivite = idActivite;
        this.villeUser = villeUser;
    }

    /** Compat : sans date ni ville (chargement minimal). */
    public Reservation(int id, String statut, int nbPersonnes, int idUser, int idActivite) {
        this(id, null, statut, nbPersonnes, idUser, idActivite, null);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getDateReservation() {
        return dateReservation;
    }

    public void setDateReservation(LocalDateTime dateReservation) {
        this.dateReservation = dateReservation;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public int getNbPersonnes() {
        return nbPersonnes;
    }

    public void setNbPersonnes(int nbPersonnes) {
        this.nbPersonnes = nbPersonnes;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public int getIdActivite() {
        return idActivite;
    }

    public void setIdActivite(int idActivite) {
        this.idActivite = idActivite;
    }

    public String getVilleUser() {
        return villeUser;
    }

    public void setVilleUser(String villeUser) {
        this.villeUser = villeUser;
    }

    public String getActiviteNom() {
        return activiteNom;
    }

    public void setActiviteNom(String activiteNom) {
        this.activiteNom = activiteNom;
    }

    public String getUserDisplay() {
        return userDisplay;
    }

    public void setUserDisplay(String userDisplay) {
        this.userDisplay = userDisplay;
    }
}
