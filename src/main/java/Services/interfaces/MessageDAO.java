package Services.interfaces;

import Entities.Message;
import Utiles.MyDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {

    private final Connection connection;

    // Constructeur sécurisé, pas de recursion
    public MessageDAO() {
        this.connection = MyDB.getInstance().getConnection();
    }

    // Ajouter un message
    public boolean addMessage(Message message) {
        String query = "INSERT INTO message (type_message, contenu, statut_message, date_envoi, id_conversation, id_user) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, message.getTypeMessage());
            ps.setString(2, message.getContenu());
            ps.setString(3, message.getStatutMessage());
            ps.setTimestamp(4, Timestamp.valueOf(message.getDateEnvoi()));
            ps.setInt(5, message.getIdConversation());
            ps.setInt(6, message.getIdUser());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("❌ Error adding message: " + e.getMessage());
            return false;
        }
    }

    // Récupérer tous les messages d'une conversation
    public List<Message> getMessagesByConversation(int idConversation) {
        List<Message> messages = new ArrayList<>();
        String query = "SELECT * FROM message WHERE id_conversation = ? ORDER BY date_envoi ASC";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, idConversation);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Message message = new Message(
                        rs.getInt("id_message"),
                        rs.getString("type_message"),
                        rs.getString("contenu"),
                        rs.getString("statut_message"),
                        rs.getTimestamp("date_envoi").toLocalDateTime(),
                        rs.getTimestamp("date_lecture") != null ? rs.getTimestamp("date_lecture").toLocalDateTime() : null,
                        rs.getInt("id_conversation"),
                        rs.getInt("id_user")
                );
                messages.add(message);
            }
        } catch (SQLException e) {
            System.out.println("❌ Error fetching messages: " + e.getMessage());
        }
        return messages;
    }

    // Marquer les messages comme lus
    public void markAsRead(int idConversation, int currentUserId) {
        String query = "UPDATE message SET statut_message = 'LU', date_lecture = ? " +
                "WHERE id_conversation = ? AND id_user != ? AND statut_message != 'LU'";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, idConversation);
            ps.setInt(3, currentUserId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("❌ Error marking as read: " + e.getMessage());
        }
    }

    // Supprimer un message
    public boolean deleteMessage(int idMessage) {
        String query = "DELETE FROM message WHERE id_message = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, idMessage);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("❌ Error deleting message: " + e.getMessage());
            return false;
        }
    }

    // Modifier le contenu d'un message
    public boolean updateMessageContent(int idMessage, String newContent) {
        String query = "UPDATE message SET contenu = ? WHERE id_message = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, newContent);
            ps.setInt(2, idMessage);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("❌ Error updating message: " + e.getMessage());
            return false;
        }
    }

    public boolean addVocalMessage(String filePath, int idConversation, int idUser) {
        String query = "INSERT INTO message (type_message, contenu, statut_message, date_envoi, id_conversation, id_user) " +
                "VALUES ('VOCAL', ?, 'ENVOYE', NOW(), ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, filePath);
            ps.setInt(2, idConversation);
            ps.setInt(3, idUser);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("❌ Error adding vocal message: " + e.getMessage());
            return false;
        }
    }

}
