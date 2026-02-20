package Entities;

import enums.StatutSeance;
import java.time.LocalDate;
import java.time.LocalTime;

public class Seance {

    private int idSeance;
    private LocalDate dateSeance;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private int capacite;
    private StatutSeance statutSeance;
    private int idPlanning;
    private int idCoach;

    public Seance() {}

    // ===== Getters & Setters =====

    public int getIdSeance() {
        return idSeance;
    }

    public void setIdSeance(int idSeance) {
        this.idSeance = idSeance;
    }

    public LocalDate getDateSeance() {
        return dateSeance;
    }

    public void setDateSeance(LocalDate dateSeance) {
        this.dateSeance = dateSeance;
    }

    public LocalTime getHeureDebut() {
        return heureDebut;
    }

    public void setHeureDebut(LocalTime heureDebut) {
        this.heureDebut = heureDebut;
    }

    public LocalTime getHeureFin() {
        return heureFin;
    }

    public void setHeureFin(LocalTime heureFin) {
        this.heureFin = heureFin;
    }

    public int getCapacite() {
        return capacite;
    }

    public void setCapacite(int capacite) {
        this.capacite = capacite;
    }

    public StatutSeance getStatutSeance() {
        return statutSeance;
    }

    public void setStatutSeance(StatutSeance statutSeance) {
        this.statutSeance = statutSeance;
    }

    public int getIdPlanning() {
        return idPlanning;
    }

    public void setIdPlanning(int idPlanning) {
        this.idPlanning = idPlanning;
    }

    public int getIdCoach() {
        return idCoach;
    }

    public void setIdCoach(int idCoach) {
        this.idCoach = idCoach;
    }

    // ===== toString =====
    @Override
    public String toString() {
        return dateSeance + " | " + heureDebut + "-" + heureFin + " | " + statutSeance;
    }
}
