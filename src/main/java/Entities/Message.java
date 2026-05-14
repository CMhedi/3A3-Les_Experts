package Entities;

import java.time.LocalDateTime;

public class Message {
    private int idMessage;
    private String typeMessage;
    private String contenu;
    private String statutMessage;
    private LocalDateTime dateEnvoi;
    private LocalDateTime dateLecture;
    private int idConversation;
    private int idUser;
    private LocalDateTime dateModifier;
    private String reactions;
    private String attachments;
    private String prioriteMessage;

    public Message() {}

    public Message(int idMessage, String typeMessage, String contenu, String statutMessage,
                   LocalDateTime dateEnvoi, LocalDateTime dateLecture, int idConversation, int idUser) {
        this.idMessage = idMessage;
        this.typeMessage = typeMessage;
        this.contenu = contenu;
        this.statutMessage = statutMessage;
        this.dateEnvoi = dateEnvoi;
        this.dateLecture = dateLecture;
        this.idConversation = idConversation;
        this.idUser = idUser;
        this.prioriteMessage = "NORMAL";
    }

    public Message(String typeMessage, String contenu, String statutMessage,
                   LocalDateTime dateEnvoi, int idConversation, int idUser) {
        this.typeMessage = typeMessage;
        this.contenu = contenu;
        this.statutMessage = statutMessage;
        this.dateEnvoi = dateEnvoi;
        this.idConversation = idConversation;
        this.idUser = idUser;
        this.prioriteMessage = "NORMAL";
    }

    public int getIdMessage() { return idMessage; }
    public void setIdMessage(int idMessage) { this.idMessage = idMessage; }
    public String getTypeMessage() { return typeMessage; }
    public void setTypeMessage(String typeMessage) { this.typeMessage = typeMessage; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public String getStatutMessage() { return statutMessage; }
    public void setStatutMessage(String statutMessage) { this.statutMessage = statutMessage; }
    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }
    public LocalDateTime getDateLecture() { return dateLecture; }
    public void setDateLecture(LocalDateTime dateLecture) { this.dateLecture = dateLecture; }
    public int getIdConversation() { return idConversation; }
    public void setIdConversation(int idConversation) { this.idConversation = idConversation; }
    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }

    public LocalDateTime getDateModifier() { return dateModifier; }
    public void setDateModifier(LocalDateTime dateModifier) { this.dateModifier = dateModifier; }
    public String getReactions() { return reactions; }
    public void setReactions(String reactions) { this.reactions = reactions; }
    public String getAttachments() { return attachments; }
    public void setAttachments(String attachments) { this.attachments = attachments; }
    public String getPrioriteMessage() { return prioriteMessage; }
    public void setPrioriteMessage(String prioriteMessage) { this.prioriteMessage = prioriteMessage; }
}
