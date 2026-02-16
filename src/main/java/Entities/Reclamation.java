package Entities;

import enums.StatutReclamation;
import java.time.LocalDateTime;

public class Reclamation {

    private int idReclamation;
    private String type;
    private String contenu;
    private StatutReclamation statut;
    private LocalDateTime dateCreation;
    private String reponse;
    private int idUser;
    private String userName;
    public Reclamation() {}

    // ===== Getters & Setters =====
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
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

    public String getReponse() { return reponse; }
    public void setReponse(String reponse) { this.reponse = reponse; }
    // ===== toString =====
    @Override
    public String toString() {
        return "Reclamation #" + idReclamation + " | " + type + " | " + statut;
    }
}
