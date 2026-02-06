package Entities;

import enums.StatutReclamation;
import java.time.LocalDateTime;

public class Reclamation {

    private int idReclamation;
    private String type;
    private String contenu;
    private StatutReclamation statut;
    private LocalDateTime dateCreation;
    private int idUser;

    public Reclamation() {}

    // ===== Getters & Setters =====

    public int getIdReclamation() {
        return idReclamation;
    }

    public void setIdReclamation(int idReclamation) {
        this.idReclamation = idReclamation;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public StatutReclamation getStatut() {
        return statut;
    }

    public void setStatut(StatutReclamation statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    // ===== toString =====
    @Override
    public String toString() {
        return "Reclamation #" + idReclamation + " | " + type + " | " + statut;
    }
}
