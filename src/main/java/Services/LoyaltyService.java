package Services;

import Utiles.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoyaltyService {

    // ✅ discount حسب عدد packs distinct اللي خذاهم user
    public int discountPercentForUser(int userId) {
        int packsCount = countDistinctPacksForUser(userId);

        if (packsCount >= 6) return 15;
        if (packsCount >= 4) return 10;
        if (packsCount >= 2) return 5;
        return 0;
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