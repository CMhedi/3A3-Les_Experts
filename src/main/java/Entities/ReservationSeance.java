package Entities;

import enums.StatutPresence;
import enums.StatutReservationSeance;
import java.time.LocalDateTime;

public class ReservationSeance {

    private int idReservation;
    private LocalDateTime dateReservation;
    private StatutReservationSeance statut;
    private UserApp user;
    private int idUser;
    private int idSeance;
    private StatutPresence statutPresence;
    // 🔥 Google Calendar
    private String googleEventId;
    private String googleEventLink;

    // =====================
    // Constructors
    // =====================

    public ReservationSeance() {
    }

    public ReservationSeance(int idUser, int idSeance) {
        this.idUser = idUser;
        this.idSeance = idSeance;
        this.statut = StatutReservationSeance.CONFIRMEE;
        this.dateReservation = LocalDateTime.now();
        this.statutPresence = StatutPresence.NON_MARQUE;
    }

    public ReservationSeance(int idReservation,
                             LocalDateTime dateReservation,
                             StatutReservationSeance statut,
                             int idUser,
                             int idSeance,
                             String googleEventId,
                             String googleEventLink) {

        this.idReservation = idReservation;
        this.dateReservation = dateReservation;
        this.statut = statut;
        this.idUser = idUser;
        this.idSeance = idSeance;
        this.googleEventId = googleEventId;
        this.googleEventLink = googleEventLink;
        this.statutPresence = StatutPresence.NON_MARQUE;
    }

    // =====================
    // Getters & Setters
    // =====================
    public UserApp getUser() {
        return user;
    }

    public void setUser(UserApp user) {
        this.user = user;
    }

    public int getIdReservation() {
        return idReservation;
    }

    public void setIdReservation(int idReservation) {
        this.idReservation = idReservation;
    }

    public LocalDateTime getDateReservation() {
        return dateReservation;
    }

    public void setDateReservation(LocalDateTime dateReservation) {
        this.dateReservation = dateReservation;
    }

    public StatutReservationSeance getStatut() {
        return statut;
    }

    public void setStatut(StatutReservationSeance statut) {
        this.statut = statut;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public int getIdSeance() {
        return idSeance;
    }

    public void setIdSeance(int idSeance) {
        this.idSeance = idSeance;
    }

    // 🔥 Google Event ID
    public String getGoogleEventId() {
        return googleEventId;
    }

    public void setGoogleEventId(String googleEventId) {
        this.googleEventId = googleEventId;
    }

    // 🔥 Google Event Link
    public String getGoogleEventLink() {
        return googleEventLink;
    }

    public void setGoogleEventLink(String googleEventLink) {
        this.googleEventLink = googleEventLink;
    }
    public StatutPresence getStatutPresence() {
        return statutPresence;
    }

    public void setStatutPresence(StatutPresence statutPresence) {
        this.statutPresence = statutPresence;
    }
    // =====================
    // toString
    // =====================

    @Override
    public String toString() {
        return "ReservationSeance{" +
                "idReservation=" + idReservation +
                ", dateReservation=" + dateReservation +
                ", statut=" + statut +
                ", idUser=" + idUser +
                ", idSeance=" + idSeance +
                ", googleEventId='" + googleEventId + '\'' +
                ", googleEventLink='" + googleEventLink + '\'' +
                '}';
    }


}