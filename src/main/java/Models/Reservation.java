package Models;

import java.sql.Date;
import java.time.LocalDateTime;

public class Reservation {

    private int id;               // id_res_act
    private Date dateReservation;  // ✅ add (ex: date_reservation)

    private String statut;        // statut_res
    private int nbPersonnes;      // nb_personnes
    private int idUser;           // id_user
    private int idActivite;       // id_activite

    private String ticketToken;
    private LocalDateTime ticketGeneratedAt;
    private boolean checkedIn;
    private LocalDateTime checkinTime;

    public Reservation() {}

    // ✅ NEW constructor that your test wants
    public Reservation(int id, Date dateReservation, String statut, int nbPersonnes, int idUser, int idActivite) {
        this.id = id;
        this.dateReservation = dateReservation;
        this.statut = statut;
        this.nbPersonnes = nbPersonnes;
        this.idUser = idUser;
        this.idActivite = idActivite;
    }

    // keep your old constructor if other code uses it
    public Reservation(int id, String statut, int nbPersonnes, int idUser, int idActivite) {
        this.id = id;
        this.statut = statut;
        this.nbPersonnes = nbPersonnes;
        this.idUser = idUser;
        this.idActivite = idActivite;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Date getDateReservation() { return dateReservation; }
    public void setDateReservation(Date dateReservation) { this.dateReservation = dateReservation; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public int getNbPersonnes() { return nbPersonnes; }
    public void setNbPersonnes(int nbPersonnes) { this.nbPersonnes = nbPersonnes; }

    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }

    public int getIdActivite() { return idActivite; }
    public void setIdActivite(int idActivite) { this.idActivite = idActivite; }

    public String getTicketToken() { return ticketToken; }
    public void setTicketToken(String ticketToken) { this.ticketToken = ticketToken; }

    public LocalDateTime getTicketGeneratedAt() { return ticketGeneratedAt; }
    public void setTicketGeneratedAt(LocalDateTime ticketGeneratedAt) { this.ticketGeneratedAt = ticketGeneratedAt; }

    public boolean isCheckedIn() { return checkedIn; }
    public void setCheckedIn(boolean checkedIn) { this.checkedIn = checkedIn; }

    public LocalDateTime getCheckinTime() { return checkinTime; }
    public void setCheckinTime(LocalDateTime checkinTime) { this.checkinTime = checkinTime; }
}