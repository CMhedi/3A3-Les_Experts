package Services;

import Entities.ReservationSeance;
import Entities.Seance;
import Entities.UserApp;
import enums.StatutPresence;
import enums.StatutReservationSeance;
import exceptions.ValidationException;
import Utiles.MyDB;
import Services.validation.ReservationSeanceValidator;

import java.sql.*;
import java.util.*;

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

            String checkSql = """
                SELECT statut
                FROM reservation_seance
                WHERE id_user = ? AND id_seance = ?
                """;

            PreparedStatement check = conx.prepareStatement(checkSql);
            check.setInt(1, idUser);
            check.setInt(2, idSeance);

            ResultSet rs = check.executeQuery();

            if (rs.next()) {

                String statut = rs.getString("statut");

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

                throw new ValidationException(
                        "Vous avez déjà réservé cette séance.");
            }

            ReservationSeance reservation =
                    new ReservationSeance(idUser, idSeance);

            ReservationSeanceValidator
                    .validateReservation(conx, reservation);

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

        } catch (SQLException e) {
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
    // Google Calendar
    // =========================

    public void saveGoogleEventId(int userId, int seanceId, String eventId) {

        String sql = """
                UPDATE reservation_seance
                SET google_event_id = ?
                WHERE id_user = ? AND id_seance = ?
                """;

        try {
            PreparedStatement ps = conx.prepareStatement(sql);
            ps.setString(1, eventId);
            ps.setInt(2, userId);
            ps.setInt(3, seanceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveGoogleEventLink(int userId, int seanceId, String eventLink) {

        String sql = """
                UPDATE reservation_seance
                SET google_event_link = ?
                WHERE id_user = ? AND id_seance = ?
                """;

        try {
            PreparedStatement ps = conx.prepareStatement(sql);
            ps.setString(1, eventLink);
            ps.setInt(2, userId);
            ps.setInt(3, seanceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getGoogleEventId(int userId, int seanceId) {

        String sql = """
                SELECT google_event_id
                FROM reservation_seance
                WHERE id_user = ?
                AND id_seance = ?
                AND statut = 'CONFIRMEE'
                """;

        try {
            PreparedStatement ps = conx.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, seanceId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("google_event_id");
            }
            return null;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getGoogleEventLink(int userId, int seanceId) {

        String sql = """
                SELECT google_event_link
                FROM reservation_seance
                WHERE id_user = ?
                AND id_seance = ?
                AND statut = 'CONFIRMEE'
                """;

        try {
            PreparedStatement ps = conx.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, seanceId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("google_event_link");
            }
            return null;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // =========================
    // Utilitaires
    // =========================

    public boolean exists(int userId, int seanceId){

        String sql = """
                SELECT COUNT(*)
                FROM reservation_seance
                WHERE id_user = ?
                AND id_seance = ?
                AND statut = 'CONFIRMEE'
                """;

        try {
            PreparedStatement ps = conx.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, seanceId);

            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int countReservations(int idSeance) {
        String sql = """
                SELECT COUNT(*)
                FROM reservation_seance
                WHERE id_seance = ?
                AND statut = 'CONFIRMEE'
                """;

        try {
            PreparedStatement ps = conx.prepareStatement(sql);
            ps.setInt(1, idSeance);

            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    };
    public void updatePresence(int idReservation,
                               StatutPresence statut) {

        String sql =
                "UPDATE reservation_seance " +
                        "SET statut_presence=? " +
                        "WHERE id_reservation=?";

        try {
            PreparedStatement ps = conx.prepareStatement(sql);
            ps.setString(1, statut.name());
            ps.setInt(2, idReservation);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public StatutPresence getPresence(int userId, int seanceId) {

        String sql = """
        SELECT statut_presence
        FROM reservation_seance
        WHERE id_user = ? AND id_seance = ?
    """;

        try {
            PreparedStatement ps = conx.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, seanceId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return StatutPresence.valueOf(
                        rs.getString("statut_presence")
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return StatutPresence.NON_MARQUE;
    }

    public List<ReservationSeance> getReservationsBySeance(int seanceId) {

        List<ReservationSeance> list = new ArrayList<>();

        String sql = """
        SELECT r.id_reservation,
               r.id_user,
               r.google_event_id,
               r.statut_presence,
               u.nom,
               u.prenom,
               u.email
        FROM reservation_seance r
        JOIN user_app u ON r.id_user = u.id_user
        WHERE r.id_seance = ?
        AND r.statut = 'CONFIRMEE'
    """;

        try {

            PreparedStatement ps = conx.prepareStatement(sql);
            ps.setInt(1, seanceId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                UserApp user = new UserApp();
                user.setIdUser(rs.getInt("id_user"));
                user.setNom(rs.getString("nom"));
                user.setPrenom(rs.getString("prenom"));
                user.setEmail(rs.getString("email"));

                ReservationSeance r = new ReservationSeance();
                r.setIdReservation(rs.getInt("id_reservation"));
                r.setIdUser(rs.getInt("id_user"));
                r.setGoogleEventId(rs.getString("google_event_id")); // 🔥 IMPORTANT
                r.setStatutPresence(
                        StatutPresence.valueOf(
                                rs.getString("statut_presence")
                        )
                );

                r.setUser(user);

                list.add(r);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }
    public int countByPresence(int idSeance, StatutPresence statut) {

        String sql = """
            SELECT COUNT(*)
            FROM reservation_seance
            WHERE id_seance = ?
            AND statut_presence = ?
            """;

        try {

            PreparedStatement ps = conx.prepareStatement(sql);
            ps.setInt(1, idSeance);
            ps.setString(2, statut.name());

            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public Map<Integer, Set<Integer>> getAllUserReservations() throws SQLException {

        Map<Integer, Set<Integer>> userMap = new HashMap<>();

        String sql = """
        SELECT id_user, id_seance
        FROM reservation_seance
        WHERE statut = 'CONFIRMEE'
    """;

        Statement st = conx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {

            int userId = rs.getInt("id_user");
            int seanceId = rs.getInt("id_seance");

            userMap
                    .computeIfAbsent(userId, k -> new HashSet<>())
                    .add(seanceId);
        }

        return userMap;
    }
    public List<Seance> getConfirmedSeancesByUser(int userId) throws SQLException {

        List<Seance> list = new ArrayList<>();

        String sql = """
        SELECT s.*
        FROM seance s
        JOIN reservation_seance r
        ON s.id_seance = r.id_seance
        WHERE r.id_user = ?
        AND r.statut = 'CONFIRMEE'
    """;

        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {

            Seance s = new Seance();

            s.setIdSeance(rs.getInt("id_seance"));
            s.setNom(rs.getString("nom"));
            s.setDateSeance(rs.getDate("date_seance").toLocalDate());
            s.setHeureDebut(rs.getTime("heure_debut").toLocalTime());
            s.setHeureFin(rs.getTime("heure_fin").toLocalTime());
            s.setCapacite(rs.getInt("capacite"));
            s.setIdCoach(rs.getInt("id_coach"));

            list.add(s);
        }

        return list;
    }

}