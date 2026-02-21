package Services;

import Models.Reservation;
import Models.TopActivite;
import Services.interfaces.IGenericService;
import utils.DataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationService implements IGenericService<Reservation> {

    private final Connection cnx = DataBase.getInstance().getConx();



    /**  DB ENUM: EN_ATTENTE / CONFIRMEE / ANNULEE */
    private String normalizeStatut(String statut) {
        if (statut == null) return "EN_ATTENTE";

        String s = statut.trim();

        // If UI labels are passed, map them
        if (s.equalsIgnoreCase("En attente")) return "EN_ATTENTE";
        if (s.equalsIgnoreCase("Confirmée") || s.equalsIgnoreCase("Confirmee")) return "CONFIRMEE";
        if (s.equalsIgnoreCase("Annulée") || s.equalsIgnoreCase("Annulee")) return "ANNULEE";

        // Otherwise normalize (EN_ATTENTE)
        s = s.toUpperCase().replace(" ", "_");
        if (s.equals("EN_ATTENTE") || s.equals("CONFIRMEE") || s.equals("ANNULEE")) return s;

        // par defaut
        return "EN_ATTENTE";
    }

    //

    @Override
    public void add(Reservation r) {
        String sql = "INSERT INTO reservation_activite (statut_res, nb_personnes, id_user, id_activite) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
           // pst.setDate(1, r.getDate());
            pst.setString(2, normalizeStatut(r.getStatut()));
            pst.setInt(3, r.getNbPersonnes());
            pst.setInt(4, r.getIdUser());
            pst.setInt(5, r.getIdActivite());
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur add reservation: " + e.getMessage(), e);
        }
    }

    /** Useful for tests: returns the generated id_res_act */
    public int addAndReturnId(Reservation r) {
        String sql = "INSERT INTO reservation_activite (statut_res, nb_personnes, id_user, id_activite) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
           // pst.setDate(1, r.getDate());
            pst.setString(2, normalizeStatut(r.getStatut()));
            pst.setInt(3, r.getNbPersonnes());
            pst.setInt(4, r.getIdUser());
            pst.setInt(5, r.getIdActivite());
            pst.executeUpdate();

            try (ResultSet keys = pst.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
            throw new RuntimeException("No generated key returned (id_res_act).");
        } catch (SQLException e) {
            throw new RuntimeException("Erreur addAndReturnId reservation: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Reservation r) {
        String sql = "UPDATE reservation_activite " +
                "SET statut_res=?, nb_personnes=?, id_user=?, id_activite=? " +
                "WHERE id_res_act=?";

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
//  pst.setDate(1, r.getDate());
            pst.setString(2, normalizeStatut(r.getStatut()));
            pst.setInt(3, r.getNbPersonnes());
            pst.setInt(4, r.getIdUser());
            pst.setInt(5, r.getIdActivite());
            pst.setInt(6, r.getId()); // your model uses getId()
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur update reservation: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM reservation_activite WHERE id_res_act=?";

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur delete reservation: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Reservation> getAll() {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation_activite";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Reservation r = new Reservation(
                        rs.getInt("id_res_act"),
                       // rs.getDate("date_reservation"),
                        rs.getString("statut_res"),
                        rs.getInt("nb_personnes"),
                        rs.getInt("id_user"),
                        rs.getInt("id_activite")
                );
                list.add(r);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur getAll reservations: " + e.getMessage(), e);
        }

        return list;
    }

    @Override
    public Reservation getById(int id) {
        String sql = "SELECT * FROM reservation_activite WHERE id_res_act=?";

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return new Reservation(
                            rs.getInt("id_res_act"),
                           // rs.getDate("date_reservation"),
                            rs.getString("statut_res"),
                            rs.getInt("nb_personnes"),
                            rs.getInt("id_user"),
                            rs.getInt("id_activite")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur getById reservation: " + e.getMessage(), e);
        }

        return null;
    }

    @Override
    public List<TopActivite> getTop3Activites() {
        return List.of();
    }

    //

    public void ajouter(Reservation r) { add(r); }
    public void modifier(Reservation r) { update(r); }
    public void supprimer(int id) { delete(id); }
    public List<Reservation> afficher() { return getAll(); }
}
