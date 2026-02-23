package Services;

import Entities.UserApp;
import Utiles.MyDB;
import enums.RoleUser;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserService implements IGenericService<UserApp> {
    private final Connection cnx = MyDB.getInstance().getConnection();

    @Override
    public void add(UserApp u) throws SQLException {
        String req = "INSERT INTO user_app (nom, prenom, email, telephone, image_url, role, mot_de_passe, age, experience, specialite, bio_certifs, disponibilite) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getTelephone());
        ps.setString(5, u.getImageUrl());
        ps.setString(6, safeRoleName(u.getRole()));
        ps.setString(7, u.getMotDePasse());

        if (u.getRole() == RoleUser.COACH) {
            ps.setInt(8, u.getAge());
            ps.setString(9, u.getExperience());
            ps.setString(10, u.getSpecialite());
            ps.setString(11, u.getBioCertifs());
            ps.setString(12, u.getDisponibilite());
        } else {
            ps.setNull(8, Types.INTEGER);
            ps.setNull(9, Types.VARCHAR);
            ps.setNull(10, Types.VARCHAR);
            ps.setNull(11, Types.VARCHAR);
            ps.setNull(12, Types.VARCHAR);
        }

        ps.executeUpdate();
        System.out.println("✅ Utilisateur ajouté avec succès !");
    }

    @Override
    public List<UserApp> getAll() throws SQLException {
        List<UserApp> list = new ArrayList<>();
        String req = "SELECT * FROM user_app";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            list.add(mapResultSetToUser(rs));
        }
        return list;
    }

    @Override
    public void update(UserApp u) throws SQLException {
        String req = "UPDATE user_app SET nom=?, prenom=?, email=?, telephone=?, image_url=?, mot_de_passe=?, role=?, age=?, experience=?, specialite=?, bio_certifs=?, disponibilite=? WHERE id_user=?";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getTelephone());
        ps.setString(5, u.getImageUrl());
        ps.setString(6, u.getMotDePasse());
        ps.setString(7, safeRoleName(u.getRole()));

        ps.setInt(8, u.getAge());
        ps.setString(9, u.getExperience());
        ps.setString(10, u.getSpecialite());
        ps.setString(11, u.getBioCertifs());
        ps.setString(12, u.getDisponibilite());

        ps.setInt(13, u.getIdUser());

        ps.executeUpdate();
        System.out.println("✅ User mis à jour avec succès !");
    }

    @Override
    public void delete(int id) throws SQLException {
        String req = "DELETE FROM user_app WHERE id_user = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ User supprimé !");
        }
    }

    @Override
    public UserApp getById(int id) throws SQLException {
        String req = "SELECT * FROM user_app WHERE id_user = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapResultSetToUser(rs);
        }
        return null;
    }

    public UserApp findByEmail(String email) {
        try {
            String query = "SELECT * FROM user_app WHERE email = ?";
            PreparedStatement ps = cnx.prepareStatement(query);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur findByEmail: " + e.getMessage());
        }
        return null;
    }

    public void updatePassword(String email, String newPassword) throws SQLException {
        String sql = "UPDATE user_app SET mot_de_passe = ? WHERE email = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setString(2, email);
            ps.executeUpdate();
        }
    }

    private UserApp mapResultSetToUser(ResultSet rs) throws SQLException {
        UserApp u = new UserApp();
        u.setIdUser(rs.getInt("id_user"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setTelephone(rs.getString("telephone"));
        u.setImageUrl(rs.getString("image_url"));
        u.setMotDePasse(rs.getString("mot_de_passe"));

        // ✅ FIX: role ممكن يكون NULL/فارغ/مختلف في DB
        u.setRole(parseRole(rs.getString("role")));

        u.setAge(rs.getInt("age"));
        u.setExperience(rs.getString("experience"));
        u.setSpecialite(rs.getString("specialite"));
        u.setBioCertifs(rs.getString("bio_certifs"));
        u.setDisponibilite(rs.getString("disponibilite"));

        return u;
    }

    public List<UserApp> getAllCoachs() {
        List<UserApp> list = new ArrayList<>();
        String req = "SELECT * FROM user_app WHERE role = 'COACH'";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur getAllCoachs: " + e.getMessage());
        }
        return list;
    }

    // ===================== SAFE ROLE PARSING =====================

    private RoleUser defaultRole() {
        String[] candidates = {"USER", "CLIENT", "MEMBER", "ADMIN", "COACH"};
        for (String c : candidates) {
            try { return RoleUser.valueOf(c); } catch (Exception ignored) {}
        }
        return (RoleUser.values().length > 0) ? RoleUser.values()[0] : null;
    }

    private String safeRoleName(RoleUser r) {
        if (r != null) return r.name();
        RoleUser d = defaultRole();
        return d != null ? d.name() : null;
    }

    private RoleUser parseRole(String raw) {
        if (raw == null) return defaultRole();

        String v = raw.trim();
        if (v.isEmpty()) return defaultRole();

        v = v.toUpperCase(Locale.ROOT)
                .replace("ROLE_", "")
                .replace("-", "_")
                .replace(" ", "_");

        if (v.equals("UTILISATEUR") || v.equals("CUSTOMER")) v = "USER";
        if (v.equals("ADMINISTRATEUR")) v = "ADMIN";

        try {
            return RoleUser.valueOf(v);
        } catch (Exception e) {
            System.out.println("⚠️ Role غير معروف في DB: '" + raw + "' => fallback");
            return defaultRole();
        }
    }
}