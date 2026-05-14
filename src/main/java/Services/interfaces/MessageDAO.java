package Services.interfaces;

import Entities.Message;
import Utiles.MyDB;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
                Timestamp dm = rs.getTimestamp("date_modifier");
                if (dm != null) {
                    message.setDateModifier(dm.toLocalDateTime());
                }
                message.setReactions(rs.getString("reactions"));
                message.setAttachments(rs.getString("attachments"));
                String pr = rs.getString("priorite_message");
                if (pr != null) {
                    message.setPrioriteMessage(pr);
                }
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

    public static final class TopUserMsg {
        private final String name;
        private final long count;

        public TopUserMsg(String name, long count) {
            this.name = name;
            this.count = count;
        }

        public String name() {
            return name;
        }

        public long count() {
            return count;
        }
    }

    /** Nombre de messages envoyés dans les {@code days} derniers jours. */
    public long countMessagesLastDays(int days) {
        String q = "SELECT COUNT(*) FROM message WHERE date_envoi >= DATE_SUB(NOW(), INTERVAL ? DAY)";
        try (PreparedStatement ps = connection.prepareStatement(q)) {
            ps.setInt(1, days);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.out.println("countMessagesLastDays: " + e.getMessage());
        }
        return 0;
    }

    /** Index 0 = J-6, index 6 = aujourd'hui (comptage par jour calendaire). */
    public int[] getDailyMessageCountsLast7Days() {
        int[] counts = new int[7];
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        String sql = "SELECT DATE(date_envoi) AS d, COUNT(*) AS cnt FROM message "
                + "WHERE DATE(date_envoi) BETWEEN ? AND ? GROUP BY DATE(date_envoi)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(start));
            ps.setDate(2, Date.valueOf(end));
            Map<LocalDate, Integer> map = new HashMap<>();
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Date d = rs.getDate("d");
                if (d != null) {
                    map.put(d.toLocalDate(), rs.getInt("cnt"));
                }
            }
            for (int i = 0; i < 7; i++) {
                counts[i] = map.getOrDefault(start.plusDays(i), 0);
            }
        } catch (SQLException e) {
            System.out.println("getDailyMessageCountsLast7Days: " + e.getMessage());
        }
        return counts;
    }

    /** Comptages par {@code type_message} sur la période. */
    public Map<String, Long> countByTypeMessageLastDays(int days) {
        Map<String, Long> map = new LinkedHashMap<>();
        String sql = "SELECT type_message, COUNT(*) AS cnt FROM message "
                + "WHERE date_envoi >= DATE_SUB(NOW(), INTERVAL ? DAY) GROUP BY type_message ORDER BY cnt DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, days);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                map.put(rs.getString("type_message"), rs.getLong("cnt"));
            }
        } catch (SQLException e) {
            System.out.println("countByTypeMessageLastDays: " + e.getMessage());
        }
        return map;
    }

    public List<TopUserMsg> topUsersByMessagesLastDays(int days, int limit) {
        List<TopUserMsg> list = new ArrayList<>();
        String sql = "SELECT TRIM(CONCAT(COALESCE(u.nom, ''), ' ', COALESCE(u.prenom, ''))) AS nm, COUNT(*) AS cnt "
                + "FROM message m JOIN user_app u ON u.id_user = m.id_user "
                + "WHERE m.date_envoi >= DATE_SUB(NOW(), INTERVAL ? DAY) "
                + "GROUP BY m.id_user, u.nom, u.prenom ORDER BY cnt DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, days);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String nm = rs.getString("nm");
                if (nm == null) {
                    nm = "";
                }
                nm = nm.trim();
                if (nm.isEmpty()) {
                    nm = "Utilisateur";
                }
                list.add(new TopUserMsg(nm, rs.getLong("cnt")));
            }
        } catch (SQLException e) {
            System.out.println("topUsersByMessagesLastDays: " + e.getMessage());
        }
        return list;
    }

}
