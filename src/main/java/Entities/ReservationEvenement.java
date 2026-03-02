package Entities;

import enums.StatutReservation;
import java.time.LocalDateTime;

public class ReservationEvenement {

    private int idResEvt;
    private LocalDateTime dateReservation;
    private StatutReservation statutRes;
    private int nbBillets;
    private int idEvenement;
    private int idUser;
    private String nomEvenement;
    private String nomUser;
    private double prixUnitaire;
    private int note; // Note de l'évaluation (1-5)

    public ReservationEvenement() {
    }

    public int getNote() {
        return note;
    }

    public void setNote(int note) {
        this.note = note;
    }

    public double getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(double prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    public double getPrixTotal() {
        return nbBillets * prixUnitaire;
    }

    public String getNomUser() {
        return nomUser;
    }

    public void setNomUser(String nomUser) {
        this.nomUser = nomUser;
    }

    public String getNomEvenement() {
        return nomEvenement;
    }

    public void setNomEvenement(String nomEvenement) {
        this.nomEvenement = nomEvenement;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

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

    public int getIdEvenement() {
        return idEvenement;
    }

    public void setIdEvenement(int idEvenement) {
        this.idEvenement = idEvenement;
    }

    @Override
    public String toString() {
        return "ResEvt #" + idResEvt + " | " + statutRes;
    }
}