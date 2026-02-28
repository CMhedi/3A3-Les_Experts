package Services;

import Entities.Pack;
import Utiles.MyDB2;
import enums.StatutPack;
import enums.TypePack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PackService {

    /* =========================
       READ (ALL)
       ========================= */
    public List<Pack> getAll() {
        MyDB2.getInstance(); // ensure connection init

        List<Pack> list = new ArrayList<>();
        String sql = "SELECT * FROM pack";

        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (Exception e) {
            System.out.println("❌ PackService.getAll error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /* =========================
       READ (BY ID)
       ========================= */
    public Pack getById(int idPack) {
        MyDB2.getInstance();

        String sql = "SELECT * FROM pack WHERE id_pack = ?";
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, idPack);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }

        } catch (Exception e) {
            System.out.println("❌ PackService.getById error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /* =========================
       CREATE
       ========================= */

    public void add(Pack p) {
        MyDB2.getInstance();

        String sql = """
            INSERT INTO pack(nom, prix_base, reduction, nb_activites_max, type_pack, statut_pack)
            VALUES (?,?,?,?,?,?)
        """;

        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, p.getNom());
            ps.setBigDecimal(2, p.getPrixBase());
            ps.setBigDecimal(3, p.getReduction());
            ps.setInt(4, p.getNbActivitesMax());
            ps.setString(5, p.getTypePack() != null ? p.getTypePack().name() : null);
            ps.setString(6, p.getStatutPack() != null ? p.getStatutPack().name() : null);

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("PackService.add failed: " + e.getMessage(), e);
        }
    }

    /* =========================
       UPDATE
       ========================= */
    public void update(Pack p) {
        MyDB2.getInstance();

        String sql = """
            UPDATE pack
            SET nom=?, prix_base=?, reduction=?, nb_activites_max=?, type_pack=?, statut_pack=?
            WHERE id_pack=?
        """;

        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, p.getNom());
            ps.setBigDecimal(2, p.getPrixBase());
            ps.setBigDecimal(3, p.getReduction());
            ps.setInt(4, p.getNbActivitesMax());
            ps.setString(5, p.getTypePack() != null ? p.getTypePack().name() : null);
            ps.setString(6, p.getStatutPack() != null ? p.getStatutPack().name() : null);
            ps.setInt(7, p.getIdPack());

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("PackService.update failed: " + e.getMessage(), e);
        }
    }

    /* =========================
       DELETE
       ========================= */
    public void delete(int idPack) {
        MyDB2.getInstance();

        String sql = "DELETE FROM pack WHERE id_pack=?";
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, idPack);
            ps.executeUpdate();

        } catch (Exception e) {
            // إذا عندك FK constraints (inscription -> pack) ستطلع هنا
            throw new RuntimeException("PackService.delete failed: " + e.getMessage(), e);
        }
    }

    /* =========================
       Mapper
       ========================= */
    private Pack map(ResultSet rs) throws Exception {
        Pack p = new Pack();
        p.setIdPack(rs.getInt("id_pack"));
        p.setNom(rs.getString("nom"));
        p.setPrixBase(rs.getBigDecimal("prix_base"));
        p.setReduction(rs.getBigDecimal("reduction"));
        p.setNbActivitesMax(rs.getInt("nb_activites_max"));

        try { p.setTypePack(TypePack.valueOf(rs.getString("type_pack"))); } catch (Exception ignored) {}
        try { p.setStatutPack(StatutPack.valueOf(rs.getString("statut_pack"))); } catch (Exception ignored) {}

        return p;
    }
}
