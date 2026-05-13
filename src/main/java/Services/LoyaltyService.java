package Services;

import Utiles.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoyaltyService {

    // ✅ discount حسب عدد packs distinct اللي خذاهم user
    public int discountPercentForUser(int userId) {
        int packsCount = countDistinctPacksForUser(userId);

        if (packsCount >= 6) return 15;
        if (packsCount >= 4) return 10;
        if (packsCount >= 2) return 5;
        return 0;
    }

    public void addPoints(int userId, int points) {
        if (points <= 0) return;
        String sql = "UPDATE user_app SET loyalty_points = loyalty_points + ? WHERE id_user = ?";
        Connection cnx = null;
        try {
            cnx = MyDB.getConnection();
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setInt(1, points);
                ps.setInt(2, userId);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int countDistinctPacksForUser(int userId) {
        String sql = "SELECT COUNT(DISTINCT id_pack) FROM inscription WHERE id_user = ?";

        // ✅ IMPORTANT: ما تستعملش try-with-resources على Connection
        Connection cnx = null;
        try {
            cnx = MyDB.getConnection();

            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}