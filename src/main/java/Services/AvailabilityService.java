// Services/AvailabilityService.java (COMPLET) -> compatible avec CategoryAvailability(String,int,int,int,String)
package Services;

import Models.CategoryAvailability;

import java.sql.*;

public class AvailabilityService {

    private final String URL = "jdbc:mysql://localhost:3306/ecoadventure?useSSL=false&serverTimezone=UTC";
    private final String USER = "root";
    private final String PASSWORD = "";

    // ✅ Retourne disponibilité (capacity + reserved + remaining) pour UNE activité
    public CategoryAvailability fetchAvailabilityForActivite(int idActivite) throws SQLException {

        String sql =
                "SELECT " +
                        "  a.id_activite, " +
                        "  a.categorie_act, " +
                        "  COALESCE(cp.capacite_totale, 0) AS capacite_totale, " +
                        "  COALESCE(SUM(CASE " +
                        "      WHEN r.statut_res IN ('EN_ATTENTE','CONFIRMEE') THEN r.nb_personnes " +
                        "      ELSE 0 " +
                        "  END), 0) AS places_reservees " +
                        "FROM activite a " +
                        "LEFT JOIN capacity_policy cp ON cp.categorie_act = a.categorie_act " +
                        "LEFT JOIN reservation_activite r ON r.id_activite = a.id_activite " +
                        "WHERE a.id_activite = ? " +
                        "GROUP BY a.id_activite, a.categorie_act, cp.capacite_totale";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idActivite);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {

                    String categorie = rs.getString("categorie_act");
                    int capacite = rs.getInt("capacite_totale");
                    int reservees = rs.getInt("places_reservees");
                    int restantes = Math.max(0, capacite - reservees);
                    String statut = restantes > 0 ? "DISPONIBLE" : "COMPLET";

                    // ✅ IMPORTANT: ton modèle n'accepte PAS le constructeur vide
                    return new CategoryAvailability(categorie, capacite, reservees, restantes, statut);
                }
            }
        }

        // si activité introuvable
        return new CategoryAvailability("UNKNOWN", 0, 0, 0, "INTRouvable");
    }

    // ✅ FIX: calcule reserved SANS compter la réservation courante
    public int getReservedForActiviteExcludeReservation(int idActivite, int excludeReservationId) throws SQLException {

        String sql =
                "SELECT COALESCE(SUM(nb_personnes),0) " +
                        "FROM reservation_activite " +
                        "WHERE id_activite = ? " +
                        "AND statut_res IN ('EN_ATTENTE','CONFIRMEE') " +
                        "AND id_res_act <> ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idActivite);
            ps.setInt(2, excludeReservationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                return 0;
            }
        }
    }
}