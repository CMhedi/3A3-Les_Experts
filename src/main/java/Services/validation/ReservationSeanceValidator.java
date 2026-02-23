package Services.validation;

import Entities.ReservationSeance;
import exceptions.ValidationException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ReservationSeanceValidator {

    public static void validateReservation(Connection conx,
                                           ReservationSeance reservation) {

        try {

            // 1️⃣ Vérifier doublon (user déjà inscrit)
            String checkSql = """
                    SELECT COUNT(*) 
                    FROM reservation_seance
                    WHERE id_user = ?
                    AND id_seance = ?
                    AND statut = 'CONFIRMEE'
                    """;

            PreparedStatement ps = conx.prepareStatement(checkSql);
            ps.setInt(1, reservation.getIdUser());
            ps.setInt(2, reservation.getIdSeance());

            ResultSet rs = ps.executeQuery();
            rs.next();

            if (rs.getInt(1) > 0) {
                throw new ValidationException(
                        "Vous êtes déjà inscrit à cette séance"
                );
            }

            // 2️⃣ Vérifier capacité restante
            String capaciteSql = """
                    SELECT capacite
                    FROM seance
                    WHERE id_seance = ?
                    AND statut_seance = 'PLANIFIEE'
                    """;

            ps = conx.prepareStatement(capaciteSql);
            ps.setInt(1, reservation.getIdSeance());
            rs = ps.executeQuery();

            if (!rs.next()) {
                throw new ValidationException(
                        "Séance invalide ou non planifiée"
                );
            }

            int capaciteMax = rs.getInt("capacite");

            String countSql = """
                    SELECT COUNT(*) 
                    FROM reservation_seance
                    WHERE id_seance = ?
                    AND statut = 'CONFIRMEE'
                    """;

            ps = conx.prepareStatement(countSql);
            ps.setInt(1, reservation.getIdSeance());
            rs = ps.executeQuery();
            rs.next();

            int reservations = rs.getInt(1);

            if (reservations >= capaciteMax) {
                throw new ValidationException(
                        "La séance est complète"
                );
            }

        } catch (SQLException e) {
            throw new ValidationException(
                    "Erreur lors de la validation de réservation"
            );
        }
    }
}
