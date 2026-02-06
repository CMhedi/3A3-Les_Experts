package Entities;

import java.time.LocalDateTime;

public class Conversation {

    private int idConversation;
    private String titre;
    private boolean estGroupe;
    private LocalDateTime dateCreation;

    public Conversation() {}

    public int getIdConversation() { return idConversation; }
    public void setIdConversation(int idConversation) { this.idConversation = idConversation; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public boolean isEstGroupe() { return estGroupe; }
    public void setEstGroupe(boolean estGroupe) { this.estGroupe = estGroupe; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    @Override
    public String toString() {
        return titre;
    }
}
