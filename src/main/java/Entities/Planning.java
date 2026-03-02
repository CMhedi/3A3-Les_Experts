package Entities;

import enums.StatutPlanning;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
public class Planning {

    private int idPlanning;
    private String titre;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private StatutPlanning statut;   // ✅ ENUM
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Planning() {}

    // ================= GETTERS & SETTERS =================

    public int getIdPlanning() {
        return idPlanning;
    }

    public void setIdPlanning(int idPlanning) {
        this.idPlanning = idPlanning;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public StatutPlanning getStatut() {
        return statut;
    }

    public void setStatut(StatutPlanning statut) {
        this.statut = statut;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ================= MÉTHODES UTILES =================

    public String getPeriode() {
        if (dateDebut == null) return "";
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("MMMM yyyy");
        return dateDebut.format(formatter);
    }

    public boolean isActif() {
        return statut == StatutPlanning.ACTIF;
    }

    @Override
    public String toString() {
        return titre != null ? titre : getPeriode();
    }

    public void setPeriode(String trim) {
            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("MMMM yyyy");
            this.dateDebut = LocalDate.parse(trim, formatter).withDayOfMonth(1);
    }


    public long getDuree() {
        if (dateDebut == null || dateFin == null)
            return 0;

        return ChronoUnit.DAYS.between(dateDebut, dateFin) + 1;
    }
}