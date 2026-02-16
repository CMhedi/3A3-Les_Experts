package Services.interfaces;

import Entities.Reclamation;
import enums.StatutReclamation;
import Utiles.MyDB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReclamationService {
    private Connection cnx = MyDB.getInstance().getConnection();

    public void ajouter(Reclamation r) throws SQLException {
        String req = "INSERT INTO reclamation (type, contenu, statut, id_user) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, r.getType());
        ps.setString(2, r.getContenu());
        ps.setString(3, r.getStatut().name());
        ps.setInt(4, r.getIdUser());
        ps.executeUpdate();
    }

    public List<Reclamation> afficher() throws SQLException {
        List<Reclamation> list = new ArrayList<>();

        // ✅ Baddelna 'u.id' b 'u.id_user' (Wala thabbet chnoua ismha fil base)
        String req = "SELECT r.*, u.nom FROM reclamation r " +
                "JOIN user_app u ON r.id_user = u.id_user";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {

            while (rs.next()) {
                Reclamation r = new Reclamation();
                r.setIdReclamation(rs.getInt("id_reclamation"));
                r.setType(rs.getString("type"));
                r.setContenu(rs.getString("contenu"));
                r.setStatut(StatutReclamation.valueOf(rs.getString("statut")));
                r.setReponse(rs.getString("reponse"));

                // ✅ Njibu el ism mel user_app
                r.setUserName(rs.getString("nom"));

                list.add(r);
            }
        }
        return list;
    }
    public List<Reclamation> afficherParUser(int userId) throws SQLException {
        List<Reclamation> list = new ArrayList<>();
        // SQL simple men ghir JOIN khaterna na3rfou el User
        String req = "SELECT * FROM reclamation WHERE id_user = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Reclamation r = new Reclamation();
            r.setIdReclamation(rs.getInt("id_reclamation"));
            r.setType(rs.getString("type"));
            r.setContenu(rs.getString("contenu"));
            r.setStatut(StatutReclamation.valueOf(rs.getString("statut")));
            r.setReponse(rs.getString("reponse")); // ✅ Lezem ychouf el reponse
            list.add(r);
        }
        return list;
    }
    public void modifierStatut(int id, StatutReclamation s) throws SQLException {
        String req = "UPDATE reclamation SET statut = ? WHERE id_reclamation = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, s.name());
        ps.setInt(2, id);
        ps.executeUpdate();
    }

    // ✅ Jdid: Modifier contenu (CRUD User)
    public void modifier(Reclamation r) throws SQLException {
        String req = "UPDATE reclamation SET type = ?, contenu = ? WHERE id_reclamation = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, r.getType());
        ps.setString(2, r.getContenu());
        ps.setInt(3, r.getIdReclamation());
        ps.executeUpdate();
    }

    // ✅ Jdid: Supprimer (CRUD User/Admin)
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM reclamation WHERE id_reclamation = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();
    }
    public void repondre(int id, String reponse) throws SQLException {
        // Ki yjaweb, el statut ywalli TRAITEE auto
        String req = "UPDATE reclamation SET reponse = ?, statut = 'TRAITEE' WHERE id_reclamation = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, reponse);
        ps.setInt(2, id);
        ps.executeUpdate();
    }


}