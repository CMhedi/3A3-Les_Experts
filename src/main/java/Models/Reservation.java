package Models;

import java.time.LocalDateTime;

public class Reservation {

    // Primary key
    private int id; // id_res_act

    // Old fields (needed for edit/delete/popups)
    private String statut;       // statut_res
    private int nbPersonnes;     // nb_personnes
    private int idUser;          // id_user
    private int idActivite;      // id_activite

    // New ticket/check-in fields
    private String ticketToken;                  // ticket_token
    private LocalDateTime ticketGeneratedAt;     // ticket_generated_at
    private boolean checkedIn;                   // checked_in
    private LocalDateTime checkinTime;           // checkin_time

    public Reservation() {}

    // (Optional) old constructor if you still use it somewhere
    public Reservation(int id, String statut, int nbPersonnes, int idUser, int idActivite) {
        this.id = id;
        this.statut = statut;
        this.nbPersonnes = nbPersonnes;
        this.idUser = idUser;
        this.idActivite = idActivite;
    }

    // ===== getters / setters =====
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

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