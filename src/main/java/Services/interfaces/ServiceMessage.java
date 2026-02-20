package Services.interfaces;

import Entities.Message;
import Utiles.MyDB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceMessage {
    private Connection conn = MyDB.getInstance().getConnection();

    // CREATE : Envoyer un message
    public void create(Message m) throws SQLException {
        // On utilise les colonnes exactes de ta table message
        String req = "INSERT INTO message (type_message, contenu, statut_message, id_conversation, id_user, date_envoi) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(req);

        // S'assurer que m.getType() renvoie 'TEXTE', 'VOCAL' ou 'IMAGE'
        ps.setString(1, m.getTypeMessage() != null ? m.getTypeMessage() : "TEXTE");
        ps.setString(2, m.getContenu());
        ps.setString(3, "ENVOYE"); // Valeur par défaut de l'ENUM statut_message
        ps.setInt(4, m.getIdConversation());
        ps.setInt(5, m.getIdUser());
        ps.setTimestamp(6, new Timestamp(System.currentTimeMillis())); // Date actuelle

        ps.executeUpdate();
    }

    // READ : Lire les messages d'une conversation
    public List<Message> readAllByConversation(int idConv) throws SQLException {
        List<Message> list = new ArrayList<>();
        String req = "SELECT * FROM message WHERE id_conversation = ? ORDER BY date_envoi ASC";
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, idConv);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Message m = new Message();
            // CORRECTION ICI : Bien mapper chaque colonne
            m.setIdMessage(rs.getInt("id_message"));
            m.setContenu(rs.getString("contenu"));
            m.setTypeMessage(rs.getString("type_message"));
            m.setIdUser(rs.getInt("id_user")); // Ne pas utiliser setIdMessage ici !
            m.setIdConversation(rs.getInt("id_conversation"));

            list.add(m);
        }
        return list;
    }

    // UPDATE : Marquer comme Lu (Ajout important pour ton CRUD)
    public void markAsRead(int idMessage) throws SQLException {
        String req = "UPDATE message SET statut_message = 'LU', date_lecture = ? WHERE id_message = ?";
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
        ps.setInt(2, idMessage);
        ps.executeUpdate();
    }

    // DELETE : Supprimer un message
    public void delete(int idMessage) throws SQLException {
        // La contrainte fk_msg_conv avec ON DELETE CASCADE dans ta base s'occupe de la cohérence
        String req = "DELETE FROM message WHERE id_message = ?";
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, idMessage);
        ps.executeUpdate();
    }
}