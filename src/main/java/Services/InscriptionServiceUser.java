package Services;

import Utiles.MyDB2;

import java.math.BigDecimal;
import java.sql.*;

public class InscriptionServiceUser {

    public int createUserInscription(int idUser, int idPack, BigDecimal montantTotal) throws SQLException {
        String sql = "INSERT INTO inscription (statut_inscr, montant_total, id_user, id_pack) VALUES ('EN_ATTENTE', ?, ?, ?)";

        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setBigDecimal(1, montantTotal);
            ps.setInt(2, idUser);
            ps.setInt(3, idPack);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Impossible de récupérer l'id inscription généré.");
    }
}