package Models;

import java.sql.Date;

public class Reservation {

    private int id;
    private Date date;
    private String statut;
    private int nbPersonnes;
    private int idUser;
    private int idActivite;

    public Reservation(int id, Date date, String statut,
                       int nbPersonnes, int idUser, int idActivite) {
        this.id = id;
        this.date = date;
        this.statut = statut;
        this.nbPersonnes = nbPersonnes;
        this.idUser = idUser;
        this.idActivite = idActivite;
    }

    public int getId() { return id; }
    public Date getDate() { return date; }
    public String getStatut() { return statut; }
    public int getNbPersonnes() { return nbPersonnes; }
    public int getIdUser() { return idUser; }
    public int getIdActivite() { return idActivite; }
}
