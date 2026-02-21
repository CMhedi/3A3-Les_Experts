package Models;

import java.sql.Date;

public class Reservation {

    private int id;
    //private Date date;
    private String statut;
    private int nbPersonnes;
    private int idUser;
    private int idActivite;

    public Reservation(int id, String statut,
                       int nbPersonnes, int idUser, int idActivite) {
        this.id = id;
        //this.date = date;
        this.statut = statut;
        this.nbPersonnes = nbPersonnes;
        this.idUser = idUser;
        this.idActivite = idActivite;
    }

    public int getId() { return id; }
    //public Date getDate() { return date; }
    public String getStatut() { return statut; }
    public int getNbPersonnes() { return nbPersonnes; }
    public int getIdUser() { return idUser; }
    public int getIdActivite() { return idActivite; }

    public void setId(int id) { this.id = id; }
    //public void setDate(Date date) { this.date = date; }
    public void setStatut(String statut) { this.statut = statut; }
    public void setIdActivite(int idActivite) { this.idActivite = idActivite; }
    public void setNbPersonnes(int nbPersonnes) {
        this.nbPersonnes = nbPersonnes;}

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }
}
