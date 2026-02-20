package Services.interfaces;

import Entities.UserApp;
import enums.RoleUser;
import Utiles.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private Connection conx;

    public UserService() {
        MyDB.getInstance();
        conx = MyDB.getConnection();
    }

    // ➕ ajouter user (coach أو user عادي)
    public void add(UserApp u) throws SQLException {
        String sql = """
            INSERT INTO user_app
            (nom, prenom, email, telephone, role, mot_de_passe)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getTelephone());
        ps.setString(5, u.getRole().name());
        ps.setString(6, u.getMotDePasse());
        ps.executeUpdate();
    }

    // 🔍 get coach by id (باش نتاكدو اللي موجود)
    public UserApp getById(int id) throws SQLException {
        String sql = "SELECT * FROM user_app WHERE id_user=?";
        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            UserApp u = new UserApp();
            u.setIdUser(rs.getInt("id_user"));
            u.setNom(rs.getString("nom"));
            u.setPrenom(rs.getString("prenom"));
            u.setEmail(rs.getString("email"));
            u.setTelephone(rs.getString("telephone"));
            u.setRole(RoleUser.valueOf(rs.getString("role")));
            return u;
        }
        return null;
    }

    // 📄 afficher tous les coachs (optionnel)
    public List<UserApp> getAllCoachs() throws SQLException {
        List<UserApp> list = new ArrayList<>();
        String sql = "SELECT * FROM user_app WHERE role='COACH'";

        Statement st = conx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            UserApp u = new UserApp();
            u.setIdUser(rs.getInt("id_user"));
            u.setNom(rs.getString("nom"));
            u.setPrenom(rs.getString("prenom"));
            u.setRole(RoleUser.COACH);
            list.add(u);
        }
        return list;
    }

    public UserApp getUserById(int coachId) {
        String sql = "SELECT * FROM user_app WHERE id_user=?";
        try {
            PreparedStatement ps = conx.prepareStatement(sql);
            ps.setInt(1, coachId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                UserApp u = new UserApp();
                u.setIdUser(rs.getInt("id_user"));
                u.setNom(rs.getString("nom"));
                u.setPrenom(rs.getString("prenom"));
                u.setEmail(rs.getString("email"));
                u.setTelephone(rs.getString("telephone"));
                u.setRole(RoleUser.valueOf(rs.getString("role")));
                return u;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
