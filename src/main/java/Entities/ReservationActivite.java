package Entities;

import enums.StatutReservation;
import java.time.LocalDateTime;

public class ReservationActivite {

    private int idResAct;
    private LocalDateTime dateReservation;
    private StatutReservation statutRes;
    private int nbPersonnes;
    private int idUser;
    private int idActivite;

    public ReservationActivite() {}

    // ===== Getters & Setters =====

    public int getIdResAct() {
        return idResAct;
    }

    public void setIdResAct(int idResAct) {
        this.idResAct = idResAct;
    }

    public LocalDateTime getDateReservation() {
        return dateReservation;
    }

    public void setDateReservation(LocalDateTime dateReservation) {
        this.dateReservation = dateReservation;
    }

    public StatutReservation getStatutRes() {
        return statutRes;
    }

    public void setStatutRes(StatutReservation statutRes) {
        this.statutRes = statutRes;
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

    // ===== toString =====
    @Override
    public String toString() {
        return "ResAct #" + idResAct + " | " + statutRes;
    }
}
