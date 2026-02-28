package Services;

import Entities.Pack;
import Utiles.MyDB2;
import enums.StatutPack;
import enums.TypePack;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PackServiceUser {

    public List<Pack> getActivePacks() throws SQLException {
        String sql = "SELECT * FROM pack WHERE statut_pack='ACTIF' ORDER BY id_pack DESC";
        List<Pack> out = new ArrayList<>();

        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Pack p = new Pack();
                p.setIdPack(rs.getInt("id_pack"));
                p.setNom(rs.getString("nom"));

                // ✅ enums مطابقين ل Pack.java (TypePack, StatutPack) :contentReference[oaicite:4]{index=4}
                p.setTypePack(TypePack.valueOf(rs.getString("type_pack")));
                p.setPrixBase(rs.getBigDecimal("prix_base"));
                p.setReduction(rs.getBigDecimal("reduction"));
                p.setNbActivitesMax(rs.getInt("nb_activites_max"));
                p.setStatutPack(StatutPack.valueOf(rs.getString("statut_pack")));

                out.add(p);
            }
        }
        return out;
    }

    public int countInscriptionsForPack(int idPack) throws SQLException {
        String sql = "SELECT COUNT(*) FROM inscription WHERE id_pack=?";
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idPack);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}