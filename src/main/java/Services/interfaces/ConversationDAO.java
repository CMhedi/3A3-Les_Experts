package Services.interfaces;

import Entities.Conversation;
import Utiles.MyDB;

import java.sql.*;
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
                "JOIN conversation_membres cm ON u.id_user = cm.id_user " +
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
        String sqlInsert = "INSERT INTO conversation_membres (id_conversation, id_user) VALUES (?, ?)";
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
                "JOIN conversation_membres cm ON c.id_conversation = cm.id_conversation " +
                "WHERE cm.id_user = ? " +
                "ORDER BY c.id_conversation DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Conversation(
                        rs.getInt("id_conversation"),
                        rs.getString("titre"),
                        rs.getInt("est_groupe")
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
        String sql = "INSERT INTO conversation (titre, est_groupe) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getTitre());
            ps.setInt(2, c.getEstGroupe());
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
                "JOIN conversation_membres cm ON c.id_conversation = cm.id_conversation " +
                "WHERE cm.id_user = ? AND c.titre LIKE ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            ps.setString(2, "%" + query + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Conversation(rs.getInt("id_conversation"), rs.getString("titre"), rs.getInt("est_groupe")));
            }
        } catch (SQLException e) { return list; }
        return list;
    }
    public String getOtherUserName(int conversationId, int currentUserId) {
        String sql = "SELECT u.nom FROM users u " +
                "JOIN conversation_members cm ON u.id_user = cm.user_id " +
                "WHERE cm.conversation_id = ? AND u.id_user != ?";
        Connection connection = null;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, conversationId);
            stmt.setInt(2, currentUserId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("nom");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Utilisateur inconnu";
    }

}