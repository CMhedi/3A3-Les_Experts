package Services.interfaces;

import Entities.UserApp;
import enums.RoleUser;
import Utiles.MyDB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import Utiles.MyDB;

public class UserService implements IGenericService<UserApp> {
    private Connection cnx = MyDB.getInstance().getConnection();



    @Override
    public void add(UserApp u) throws SQLException {

        String req = "INSERT INTO user_app (nom, prenom, email, telephone, image_url, role, mot_de_passe) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getTelephone());
        ps.setString(5, u.getImageUrl());
        ps.setString(6, u.getRole().name());
        ps.setString(7, u.getMotDePasse());
        ps.executeUpdate();
    }

    @Override
    public List<UserApp> getAll() throws SQLException {
        List<UserApp> list = new ArrayList<>();
        String req = "SELECT * FROM user_app";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            UserApp u = new UserApp();
            u.setIdUser(rs.getInt("id_user"));
            u.setNom(rs.getString("nom"));
            u.setPrenom(rs.getString("prenom"));
            u.setEmail(rs.getString("email"));
            u.setTelephone(rs.getString("telephone"));
            u.setMotDePasse(rs.getString("mot_de_passe"));
            u.setRole(RoleUser.valueOf(rs.getString("role")));
            list.add(u);
        }
        return list;
    }

    @Override
    public void update(UserApp u) throws SQLException {

        String req = "UPDATE user_app SET nom=?, prenom=?, email=?, telephone=?, image_url=?, mot_de_passe=? ,role=? WHERE id_user=?";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getTelephone());
        ps.setString(5, u.getImageUrl());
        ps.setString(6, u.getMotDePasse());
        ps.setString(7, u.getRole().name());
        ps.setInt(8, u.getIdUser());

        ps.executeUpdate();
        System.out.println("✅ User mis à jour avec succès dans la base !");
    }

    @Override
    public void delete(int id) throws SQLException {
        String req = "DELETE FROM user_app WHERE id_user = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();
    }



    @Override
    public UserApp getById(int id) throws SQLException {
        String req = "SELECT * FROM user_app WHERE id_user = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            UserApp u = new UserApp();
            u.setIdUser(rs.getInt("id_user"));
            u.setNom(rs.getString("nom"));
            u.setPrenom(rs.getString("prenom")); // Zidha
            u.setEmail(rs.getString("email"));
            u.setTelephone(rs.getString("telephone")); // Zidha
            u.setImageUrl(rs.getString("image_url")); // Zidha
            u.setRole(RoleUser.valueOf(rs.getString("role"))); // Zidha
            return u;
        }
        return null;
    }
    public void updatePassword(String email, String newPassword) throws SQLException {

        String sql = "UPDATE user_app SET mot_de_passe = ? WHERE email = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setString(2, email);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Mot de passe mis à jour pour: " + email);
            } else {
                System.out.println("⚠️ Aucun utilisateur trouvé avec cet email.");
            }
        }
    }
    public UserApp findByEmail(String email) {
        UserApp user = null;
        try {
            String query = "SELECT * FROM user_app WHERE email = ?";
            PreparedStatement ps = cnx.prepareStatement(query);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                user = new UserApp();
                user.setIdUser(rs.getInt("id_user"));
                user.setNom(rs.getString("nom"));
                user.setPrenom(rs.getString("prenom"));
                user.setEmail(rs.getString("email"));
                user.setTelephone(rs.getString("telephone"));
                user.setMotDePasse(rs.getString("mot_de_passe"));
                user.setRole(enums.RoleUser.valueOf(rs.getString("role")));


                user.setImageUrl(rs.getString("image_url"));
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur findByEmail: " + e.getMessage());
        }
        return user;
    }
}