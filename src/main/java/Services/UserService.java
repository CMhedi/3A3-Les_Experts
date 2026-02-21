package Services;

import Entities.UserApp;
import Utiles.MyDB;
import enums.RoleUser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private static final String SQL = "SELECT * FROM user_app"; // ✅ بدّل إذا table مختلفة

    public List<UserApp> getAll() {
        List<UserApp> list = new ArrayList<>();

        try (Connection cnx = MyDB.getConnection();
             PreparedStatement ps = cnx.prepareStatement(SQL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                UserApp u = new UserApp();
                u.setIdUser(rs.getInt("id_user"));
                u.setNom(rs.getString("nom"));
                u.setPrenom(rs.getString("prenom"));
                u.setEmail(rs.getString("email"));
                u.setTelephone(rs.getString("telephone"));
                u.setImageUrl(rs.getString("image_url"));

                try { u.setRole(RoleUser.valueOf(rs.getString("role"))); } catch (Exception ignored) {}

                Timestamp ts = null;
                try { ts = rs.getTimestamp("date_creation"); } catch (Exception ignored) {}
                if (ts != null) u.setDateCreation(ts.toLocalDateTime());

                list.add(u);
            }

            System.out.println("✅ Users loaded: " + list.size());
        } catch (Exception e) {
            System.out.println("❌ UserService.getAll error: " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }
}
