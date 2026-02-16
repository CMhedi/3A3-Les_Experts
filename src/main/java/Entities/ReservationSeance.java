package Entities;

import enums.StatutReservationSeance;

import java.time.LocalDateTime;

public class ReservationSeance {

    private int idReservation;
    private LocalDateTime dateReservation;
    private StatutReservationSeance statut;

    private int idUser;
    private int idSeance;

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
    }

    public ReservationSeance(int idReservation,
                             LocalDateTime dateReservation,
                             StatutReservationSeance statut,
                             int idUser,
                             int idSeance) {
        this.idReservation = idReservation;
        this.dateReservation = dateReservation;
        this.statut = statut;
        this.idUser = idUser;
        this.idSeance = idSeance;
    }

    // =====================
    // Getters & Setters
    // =====================

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
                '}';
    }
}
