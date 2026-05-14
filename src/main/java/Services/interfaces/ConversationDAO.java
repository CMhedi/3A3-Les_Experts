package Services.interfaces;

import Entities.AdminConversationListItem;
import Entities.Conversation;
import Utiles.MyDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ConversationDAO {

    private Connection con;

    public ConversationDAO() {
        con = MyDB.getInstance().getConnection();
    }

    // --- 1. Njibou el membres mta3 Groupe (SQL msalya7 m3a user_app) ---
    public List<String> getMembresByConversation(int idConversation) {
        List<String> membres = new ArrayList<>();
        String sql = "SELECT CONCAT(u.nom, ' ', u.prenom) as fullname FROM user_app u " +
                "JOIN conversation_user cm ON u.id_user = cm.id_user " +
                "WHERE cm.id_conversation = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idConversation);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                membres.add(rs.getString("fullname"));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getMembres: " + e.getMessage());
        }
        return membres;
    }

    // --- 2. Ajouter Membre (Version Email - Lel ChoiceDialog) ---
    public boolean addMemberToConversation(int idConversation, String email) {
        String sqlGetId = "SELECT id_user FROM user_app WHERE email = ?";
        try (PreparedStatement psId = con.prepareStatement(sqlGetId)) {
            psId.setString(1, email);
            ResultSet rs = psId.executeQuery();
            if (rs.next()) {
                return addMemberToConversation(idConversation, rs.getInt("id_user"));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur addMember (Email): " + e.getMessage());
        }
        return false;
    }

    // --- NEW: Ajouter Membre (Version ID - Directe) ---
    public boolean addMemberToConversation(int idConversation, int userId) {
        String sqlInsert = "INSERT INTO conversation_user (id_conversation, id_user) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sqlInsert)) {
            ps.setInt(1, idConversation);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Kenou membre deja, SQL ya3tik erreur (Duplicate), donc nraj3ou false
            return false;
        }
    }

    // --- 3. getAllAppUsers ---
    public List<String> getAllAppUsers() {
        List<String> users = new ArrayList<>();
        String sql = "SELECT nom, prenom, email FROM user_app";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                users.add(rs.getString("nom") + " " + rs.getString("prenom") + " | " + rs.getString("email"));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getAllAppUsers: " + e.getMessage());
        }
        return users;
    }

    // --- 4. CRUD Conversation (Filtré par User) ---

    /**
     * MODIF: Njibou ken el conversations elli l'utilisateur taraf fihom
     */

    public List<Conversation> getAllConversations(int currentUserId) {
        List<Conversation> list = new ArrayList<>();
        // Query elli t-lawaj 3al id_user dakhil el table mta3 el membres
        String sql = "SELECT c.* FROM conversation c " +
                "JOIN conversation_user cm ON c.id_conversation = cm.id_conversation " +
                "WHERE cm.id_user = ? " +
                "ORDER BY c.id_conversation DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("date_creation");
                LocalDateTime dc = ts != null ? ts.toLocalDateTime() : null;
                list.add(new Conversation(
                        rs.getInt("id_conversation"),
                        rs.getString("titre"),
                        rs.getInt("est_groupe"),
                        rs.getInt("id_createur"),
                        dc
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * MODIF: addConversation t-rajja3 l'ID elli t-crea bech n-ajoutiwi l-user direct ba3dha
     */
    public int addConversation(Conversation c) {
        if (c.getIdCreateur() <= 0) {
            System.err.println("addConversation: id_createur requis (> 0)");
            return -1;
        }
        String sql = "INSERT INTO conversation (titre, est_groupe, date_creation, id_createur) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getTitre());
            ps.setInt(2, c.getEstGroupe());
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(4, c.getIdCreateur());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean updateConversationTitle(int id, String newTitle) {
        String sql = "UPDATE conversation SET titre = ? WHERE id_conversation = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newTitle);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean deleteConversation(int id) {
        // DELETE CASCADE yelzmou ykoun f-database bech y-na7i el membres zeda
        String sql = "DELETE FROM conversation WHERE id_conversation = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }




    public List<Conversation> searchConversations(String query, int currentUserId) {
        List<Conversation> list = new ArrayList<>();
        String sql = "SELECT c.* FROM conversation c " +
                "JOIN conversation_user cm ON c.id_conversation = cm.id_conversation " +
                "WHERE cm.id_user = ? AND c.titre LIKE ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            ps.setString(2, "%" + query + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("date_creation");
                LocalDateTime dc = ts != null ? ts.toLocalDateTime() : null;
                list.add(new Conversation(
                        rs.getInt("id_conversation"),
                        rs.getString("titre"),
                        rs.getInt("est_groupe"),
                        rs.getInt("id_createur"),
                        dc));
            }
        } catch (SQLException e) { return list; }
        return list;
    }

    /**
     * Toutes les conversations (écran admin), avec comptages et dernier message.
     */
    public List<AdminConversationListItem> findAllForAdmin() {
        List<AdminConversationListItem> out = new ArrayList<>();
        String sql = "SELECT c.id_conversation, c.titre, c.est_groupe, c.date_creation, "
                + "TRIM(CONCAT(COALESCE(u.nom, ''), ' ', COALESCE(u.prenom, ''))) AS createur_nom, "
                + "(SELECT COUNT(*) FROM conversation_user cu WHERE cu.id_conversation = c.id_conversation) AS nb_participants, "
                + "(SELECT COUNT(*) FROM message m WHERE m.id_conversation = c.id_conversation) AS nb_messages, "
                + "(SELECT m2.contenu FROM message m2 WHERE m2.id_conversation = c.id_conversation "
                + "ORDER BY m2.date_envoi DESC LIMIT 1) AS dernier_contenu "
                + "FROM conversation c "
                + "LEFT JOIN user_app u ON u.id_user = c.id_createur "
                + "ORDER BY c.date_creation DESC, c.id_conversation DESC";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String creator = rs.getString("createur_nom");
                if (creator == null) {
                    creator = "";
                }
                creator = creator.trim();
                if (creator.isEmpty()) {
                    creator = "Utilisateur inconnu";
                }
                Timestamp ts = rs.getTimestamp("date_creation");
                LocalDateTime dc = ts != null ? ts.toLocalDateTime() : null;
                String last = rs.getString("dernier_contenu");
                out.add(new AdminConversationListItem(
                        rs.getInt("id_conversation"),
                        rs.getString("titre"),
                        rs.getInt("est_groupe"),
                        creator,
                        rs.getInt("nb_participants"),
                        rs.getInt("nb_messages"),
                        dc,
                        last
                ));
            }
        } catch (SQLException e) {
            System.err.println("findAllForAdmin: " + e.getMessage());
        }
        return out;
    }

}