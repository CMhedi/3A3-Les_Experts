package Entities;
import java.time.LocalDateTime;

public class Message {
    private int idMessage;
    private String typeMessage;      // TEXTE, VOCAL, IMAGE
    private String contenu;
    private String statutMessage;    // ENVOYE, LU, SUPPRIME
    private LocalDateTime dateEnvoi;
    private LocalDateTime dateLecture;
    private int idConversation;
    private int idUser;

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
    }

    public Message(String typeMessage, String contenu, String statutMessage,
                   LocalDateTime dateEnvoi, int idConversation, int idUser) {
        this.typeMessage = typeMessage;
        this.contenu = contenu;
        this.statutMessage = statutMessage;
        this.dateEnvoi = dateEnvoi;
        this.idConversation = idConversation;
        this.idUser = idUser;
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
}