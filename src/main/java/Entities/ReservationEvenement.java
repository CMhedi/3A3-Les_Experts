package Entities;

import enums.StatutReservation;
import java.time.LocalDateTime;

public class ReservationEvenement {

    private int idResEvt;
    private LocalDateTime dateReservation;
    private StatutReservation statutRes;
    private int nbBillets;
    private int idUser;
    private int idEvenement;

    public ReservationEvenement() {}

    // ===== Getters & Setters =====

    public int getIdResEvt() {
        return idResEvt;
    }

    public void setIdResEvt(int idResEvt) {
        this.idResEvt = idResEvt;
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

    public int getNbBillets() {
        return nbBillets;
    }

    public void setNbBillets(int nbBillets) {
        this.nbBillets = nbBillets;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public int getIdEvenement() {
        return idEvenement;
    }

    public void setIdEvenement(int idEvenement) {
        this.idEvenement = idEvenement;
    }

    // ===== toString =====
    @Override
    public String toString() {
        return "ResEvt #" + idResEvt + " | " + statutRes;
    }
}
