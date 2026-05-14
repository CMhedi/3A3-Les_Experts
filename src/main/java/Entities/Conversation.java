package Entities;

import java.time.LocalDateTime;

public class Conversation {

    private int idConversation;
    private String titre;
    private int estGroupe;
    private LocalDateTime dateCreation;
    /** FK vers `conversation.id_createur` (obligatoire à l'insertion). */
    private int idCreateur;

    public Conversation() {}

    /** Ligne complète depuis la base (`date_creation`, `id_createur`). */
    public Conversation(int idConversation, String titre, int estGroupe, int idCreateur, LocalDateTime dateCreation) {
        this.idConversation = idConversation;
        this.titre = titre;
        this.estGroupe = estGroupe;
        this.idCreateur = idCreateur;
        this.dateCreation = dateCreation;
    }

    /** Compat : sans métadonnées créateur / date. */
    public Conversation(int idConversation, String titre, int estGroupe) {
        this(idConversation, titre, estGroupe, 0, null);
    }

    /** Nouvelle conversation : `id_createur` requis pour l'INSERT SQL. */
    public Conversation(String titre, int estGroupe, int idCreateur) {
        this.titre = titre;
        this.estGroupe = estGroupe;
        this.idCreateur = idCreateur;
    }

    /** @deprecated Préférer {@link #Conversation(String, int, int)} pour les insertions. */
    public Conversation(String titre, int estGroupe) {
        this(titre, estGroupe, 0);
    }

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

    public int getIdCreateur() {
        return idCreateur;
    }

    public void setIdCreateur(int idCreateur) {
        this.idCreateur = idCreateur;
    }

    @Override
    public String toString() {
        return "Conversation{" +
                "idConversation=" + idConversation +
                ", titre='" + titre + '\'' +
                ", estGroupe=" + estGroupe +
                ", dateCreation=" + dateCreation +
                ", idCreateur=" + idCreateur +
                '}';
    }
}
