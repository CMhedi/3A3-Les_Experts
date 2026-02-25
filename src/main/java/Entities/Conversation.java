package Entities;

import java.time.LocalDateTime;

public class Conversation {

    private int idConversation;
    private String titre;
    private int estGroupe;
    private LocalDateTime dateCreation;

    // Constructor 1: Empty constructor
    public Conversation() {}

    // Constructor 2: With all parameters (3 params) ← This is what you need!
    public Conversation(int idConversation, String titre, int estGroupe) {
        this.idConversation = idConversation;
        this.titre = titre;
        this.estGroupe = estGroupe;
    }

    // Constructor 3: With all parameters including dateCreation (4 params)
    public Conversation(int idConversation, String titre, int estGroupe, LocalDateTime dateCreation) {
        this.idConversation = idConversation;
        this.titre = titre;
        this.estGroupe = estGroupe;
        this.dateCreation = dateCreation;
    }

    // Constructor 4: For creating new conversation (2 params)
    public Conversation(String titre, int estGroupe) {
        this.titre = titre;
        this.estGroupe = estGroupe;
    }

    // Getters and Setters
    public int getIdConversation() {
        return idConversation;
    }

    public void setIdConversation(int idConversation) {
        this.idConversation = idConversation;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public int getEstGroupe() {
        return estGroupe;
    }

    public void setEstGroupe(int estGroupe) {
        this.estGroupe = estGroupe;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    @Override
    public String toString() {
        return "Conversation{" +
                "idConversation=" + idConversation +
                ", titre='" + titre + '\'' +
                ", estGroupe=" + estGroupe +
                ", dateCreation=" + dateCreation +
                '}';
    }
}