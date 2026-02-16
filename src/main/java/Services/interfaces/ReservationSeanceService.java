package Services.interfaces;

import Entities.ReservationSeance;
import enums.StatutReservationSeance;
import exceptions.ValidationException;
import Utiles.MyDB;
import Services.interfaces.validation.ReservationSeanceValidator;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationSeanceService {

    private Connection conx;

    public ReservationSeanceService() {
        MyDB.getInstance();
        conx = MyDB.getConnection();
    }
    private boolean userExiste(int idUser) throws SQLException {

        String sql = "SELECT id_user FROM user_app WHERE id_user = ?";
        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setInt(1, idUser);

        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    // =========================
    // 1️⃣ Réserver une séance
    // =========================
    public void reserver(int idUser, int idSeance)
            throws ValidationException, SQLException {
        if (!userExiste(idUser)) {
            throw new ValidationException("Utilisateur inexistant.");
        }


        try {

            // 🔍 vérifier si réservation existe
            String checkSql = """
                SELECT statut
                FROM reservation_seance
                WHERE id_user = ? AND id_seance = ?
                """;

            PreparedStatement check =
                    conx.prepareStatement(checkSql);

            check.setInt(1, idUser);
            check.setInt(2, idSeance);

            ResultSet rs = check.executeQuery();

            if (rs.next()) {

                String statut = rs.getString("statut");

                // 🔥 si annulée → on réactive
                if (statut.equals(
                        StatutReservationSeance.ANNULEE.name())) {

                    String updateSql = """
                        UPDATE reservation_seance
                        SET statut = ?
                        WHERE id_user = ? AND id_seance = ?
                        """;

                    PreparedStatement update =
                            conx.prepareStatement(updateSql);

                    update.setString(1,
                            StatutReservationSeance.CONFIRMEE.name());

                    update.setInt(2, idUser);
                    update.setInt(3, idSeance);

                    update.executeUpdate();
                    return;
                }

                // sinon déjà réservée
                throw new ValidationException(
                        "Vous avez déjà réservé cette séance.");
            }

            // 🔐 Validation métier (capacité etc.)
            ReservationSeance reservation =
                    new ReservationSeance(idUser, idSeance);

            ReservationSeanceValidator
                    .validateReservation(conx, reservation);

            // ➕ INSERT
            String insertSql = """
                INSERT INTO reservation_seance
                (date_reservation, statut, id_user, id_seance)
                VALUES (?, ?, ?, ?)
                """;

            PreparedStatement insert =
                    conx.prepareStatement(insertSql);

            insert.setTimestamp(1,
                    Timestamp.valueOf(
                            reservation.getDateReservation()));

            insert.setString(2,
                    reservation.getStatut().name());

            insert.setInt(3, idUser);
            insert.setInt(4, idSeance);

            insert.executeUpdate();

        }
        catch (SQLException e) {
            throw new RuntimeException(
                    "Erreur base de données.", e);
        }
    }



    // =========================
    // 2️⃣ Annuler réservation
    // =========================
    public void annuler(int idUser, int idSeance)
            throws ValidationException {

        try {

            String sql = """
                UPDATE reservation_seance
                SET statut = ?
                WHERE id_user = ? AND id_seance = ?
                """;

            PreparedStatement ps =
                    conx.prepareStatement(sql);

            ps.setString(1,
                    StatutReservationSeance.ANNULEE.name());

            ps.setInt(2, idUser);
            ps.setInt(3, idSeance);

            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new ValidationException(
                        "Aucune réservation trouvée.");
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erreur base de données.", e);
        }
    }


    // =========================
    // 3️⃣ Mes séances réservées
    // =========================
    public List<Integer> getSeancesReserveesByUser(int idUser) throws SQLException {

        String sql = """
                SELECT id_seance
                FROM reservation_seance
                WHERE id_user = ?
                AND statut = 'CONFIRMEE'
                """;

        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setInt(1, idUser);

        ResultSet rs = ps.executeQuery();
        List<Integer> seances = new ArrayList<>();

        while (rs.next()) {
            seances.add(rs.getInt("id_seance"));
        }
        return seances;
    }

    // =========================
    // 4️⃣ Nombre de réservations
    // =========================
    public int countReservations(int idSeance) throws SQLException {

        String sql = """
                SELECT COUNT(*) 
                FROM reservation_seance
                WHERE id_seance = ?
                AND statut = 'CONFIRMEE'
                """;

        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setInt(1, idSeance);

        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt(1);
    }


}
