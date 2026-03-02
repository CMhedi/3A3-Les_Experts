package Services;

import Entities.Planning;
import Entities.Seance;
import Entities.UserApp;
import enums.StatutPlanning;
import enums.StatutSeance;
import Services.validation.SeanceValidator;
import Utiles.MyDB;
import exceptions.ValidationException;

import java.sql.Date;
import java.time.LocalDate;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class SeanceService implements IGenericService<Seance> {

    private Connection conx;

    public SeanceService() {
        MyDB.getInstance();
        conx = MyDB.getConnection();
    }

    // ==========================
    // ADD
    // ==========================
    @Override
    public void add(Seance s) throws SQLException {

        SeanceValidator.validate(s);

        // 🔥 NOUVELLE VALIDATION MÉTIER
        validatePlanningRules(s);

        String sql = """
        INSERT INTO seance
        (nom, date_seance, heure_debut, heure_fin, capacite,
         statut_seance, id_planning, id_coach)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """;

        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setString(1, s.getNom());
        ps.setDate(2, Date.valueOf(s.getDateSeance()));
        ps.setTime(3, Time.valueOf(s.getHeureDebut()));
        ps.setTime(4, Time.valueOf(s.getHeureFin()));
        ps.setInt(5, s.getCapacite());
        ps.setString(6, s.getStatutSeance().name());
        ps.setInt(7, s.getIdPlanning());
        ps.setInt(8, s.getIdCoach());

        ps.executeUpdate();
    }

    // ==========================
    // UPDATE
    // ==========================
    @Override
    public void update(Seance s) throws SQLException {

        // 🔐 Validation métier AVANT update
        SeanceValidator.validate(s);
        validatePlanningRules(s);
        String sql = """
    UPDATE seance SET
    nom=?,
    date_seance=?,
    heure_debut=?,
    heure_fin=?,
    capacite=?,
    statut_seance=?,
    id_planning=?,
    id_coach=?
    WHERE id_seance=?
""";


        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setString(1, s.getNom());
        ps.setDate(2, Date.valueOf(s.getDateSeance()));
        ps.setTime(3, Time.valueOf(s.getHeureDebut()));
        ps.setTime(4, Time.valueOf(s.getHeureFin()));
        ps.setInt(5, s.getCapacite());
        ps.setString(6, s.getStatutSeance().name());
        ps.setInt(7, s.getIdPlanning());
        ps.setInt(8, s.getIdCoach());
        ps.setInt(9, s.getIdSeance());

        ps.executeUpdate();
    }

    // ==========================
    // DELETE
    // ==========================
    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM seance WHERE id_seance=?";

        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    // ==========================
    // GET ALL
    // ==========================
    @Override
    public List<Seance> getAll() throws SQLException {
        List<Seance> seances = new ArrayList<>();
        String sql = "SELECT * FROM seance";

        Statement st = conx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Seance s = new Seance();
            s.setNom(rs.getString("nom"));
            s.setIdSeance(rs.getInt("id_seance"));
            s.setDateSeance(rs.getDate("date_seance").toLocalDate());
            s.setHeureDebut(rs.getTime("heure_debut").toLocalTime());
            s.setHeureFin(rs.getTime("heure_fin").toLocalTime());
            s.setCapacite(rs.getInt("capacite"));
            s.setStatutSeance(
                    StatutSeance.valueOf(rs.getString("statut_seance"))
            );
            s.setIdPlanning(rs.getInt("id_planning"));
            s.setIdCoach(rs.getInt("id_coach"));

            seances.add(s);
        }
        return seances;
    }

    // ==========================
    // GET BY ID
    // ==========================
    @Override
    public Seance getById(int id) throws SQLException {
        String sql = "SELECT * FROM seance WHERE id_seance=?";
        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Seance s = new Seance();
          s.setNom(rs.getString("nom"));
            s.setIdSeance(rs.getInt("id_seance"));
            s.setDateSeance(rs.getDate("date_seance").toLocalDate());
            s.setHeureDebut(rs.getTime("heure_debut").toLocalTime());
            s.setHeureFin(rs.getTime("heure_fin").toLocalTime());
            s.setCapacite(rs.getInt("capacite"));
            s.setStatutSeance(
                    StatutSeance.valueOf(rs.getString("statut_seance"))
            );
            s.setIdPlanning(rs.getInt("id_planning"));
            s.setIdCoach(rs.getInt("id_coach"));
            return s;
        }
        return null;
    }
    public List<Seance> getByPlanning(int planningId) throws SQLException {

        List<Seance> seances = new ArrayList<>();

        String sql = "SELECT * FROM seance WHERE id_planning = ?";

        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setInt(1, planningId);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {

            Seance s = new Seance();
            s.setNom(rs.getString("nom"));
            s.setIdSeance(rs.getInt("id_seance"));
            s.setDateSeance(rs.getDate("date_seance").toLocalDate());
            s.setHeureDebut(rs.getTime("heure_debut").toLocalTime());
            s.setHeureFin(rs.getTime("heure_fin").toLocalTime());
            s.setCapacite(rs.getInt("capacite"));
            s.setStatutSeance(
                    StatutSeance.valueOf(
                            rs.getString("statut_seance")
                    )
            );
            s.setIdPlanning(rs.getInt("id_planning"));
            s.setIdCoach(rs.getInt("id_coach"));

            seances.add(s);
        }

        return seances;
    }
    public boolean hasSeanceInPlanning(int planningId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM seance WHERE id_planning = ?";
        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setInt(1, planningId);

        ResultSet rs = ps.executeQuery();
        rs.next();

        return rs.getInt(1) > 0;
    }
    public List<Seance> getByCoach(int coachId) throws Exception {
        List<Seance> list = new ArrayList<>();

        String sql = "SELECT * FROM seance WHERE id_coach=?";

        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setInt(1, coachId);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {

            Seance s = new Seance();
            s.setNom(rs.getString("nom"));
            s.setIdSeance(rs.getInt("id_seance"));
            s.setDateSeance(
                    rs.getDate("date_seance").toLocalDate());
            s.setHeureDebut(
                    rs.getTime("heure_debut").toLocalTime());
            s.setHeureFin(
                    rs.getTime("heure_fin").toLocalTime());
            s.setCapacite(rs.getInt("capacite"));
            s.setStatutSeance(
                    StatutSeance.valueOf(
                            rs.getString("statut_seance")));
            s.setIdPlanning(rs.getInt("id_planning"));
            s.setIdCoach(rs.getInt("id_coach"));

            list.add(s);
        }

        return list;
    }


    public List<Seance> getSeancesByUser(int userTestId) {
        List<Seance> seances = new ArrayList<>();

        String sql = """
                SELECT s.* 
                FROM seance s
                JOIN reservation_seance r ON s.id_seance = r.id_seance
                WHERE r.id_user = ?
                AND r.statut = 'CONFIRMEE'
                """;

        try {
            PreparedStatement ps = conx.prepareStatement(sql);
            ps.setInt(1, userTestId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Seance s = new Seance();
                s.setNom(rs.getString("nom"));
                s.setIdSeance(rs.getInt("id_seance"));
                s.setDateSeance(rs.getDate("date_seance").toLocalDate());
                s.setHeureDebut(rs.getTime("heure_debut").toLocalTime());
                s.setHeureFin(rs.getTime("heure_fin").toLocalTime());
                s.setCapacite(rs.getInt("capacite"));
                s.setStatutSeance(
                        StatutSeance.valueOf(rs.getString("statut_seance"))
                );
                s.setIdPlanning(rs.getInt("id_planning"));
                s.setIdCoach(rs.getInt("id_coach"));

                seances.add(s);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return seances;
    }
    private void validatePlanningRules(Seance s) throws SQLException {

        PlanningService planningService = new PlanningService();
        Planning planning = planningService.getById(s.getIdPlanning());

        if (planning == null)
            throw new ValidationException("Planning introuvable.");

        LocalDate today = LocalDate.now();
        LocalDate dateSeance = s.getDateSeance();

        // ❌ Séance avant aujourd'hui
        if (dateSeance.isBefore(today)) {
            throw new ValidationException(
                    "Impossible d'ajouter une séance dans le passé."
            );
        }

        // ❌ Planning ARCHIVE
        if (planning.getStatut() == StatutPlanning.ARCHIVE) {
            throw new ValidationException(
                    "Impossible d'ajouter une séance à un planning archivé."
            );
        }

        // ❌ Planning expiré
        if (planning.getDateFin().isBefore(today)) {
            throw new ValidationException(
                    "Le planning est expiré."
            );
        }

        // ❌ Date hors période planning
        if (dateSeance.isBefore(planning.getDateDebut())
                || dateSeance.isAfter(planning.getDateFin())) {

            throw new ValidationException(
                    "La date de la séance doit être comprise entre "
                            + planning.getDateDebut()
                            + " et "
                            + planning.getDateFin()
            );
        }
    }
    public double calculateTauxRemplissage(int idSeance) throws SQLException {

        String sqlCapacite = "SELECT capacite FROM seance WHERE id_seance = ?";
        PreparedStatement ps1 = conx.prepareStatement(sqlCapacite);
        ps1.setInt(1, idSeance);
        ResultSet rs1 = ps1.executeQuery();

        if (!rs1.next()) return 0;

        int capacite = rs1.getInt("capacite");

        String sqlReservations = """
        SELECT COUNT(*) 
        FROM reservation_seance 
        WHERE id_seance = ? 
        AND statut = 'CONFIRMEE'
    """;

        PreparedStatement ps2 = conx.prepareStatement(sqlReservations);
        ps2.setInt(1, idSeance);
        ResultSet rs2 = ps2.executeQuery();
        rs2.next();

        int reservations = rs2.getInt(1);

        if (capacite == 0) return 0;

        return (reservations * 100.0) / capacite;
    }
    public List<String> getTop3CoachesDetailed() throws Exception {

        SeanceService seanceService = new SeanceService();
        List<Seance> seances = seanceService.getAll();

        Map<Integer, List<Double>> coachScores = new HashMap<>();
        Map<Integer, Integer> coachSessionCount = new HashMap<>();

        for (Seance s : seances) {

            if (s.getDateSeance() != null &&
                    s.getDateSeance().isBefore(LocalDate.now())) {

                double taux = seanceService
                        .calculateTauxRemplissage(s.getIdSeance());

                coachScores
                        .computeIfAbsent(s.getIdCoach(), k -> new ArrayList<>())
                        .add(taux);

                coachSessionCount.merge(
                        s.getIdCoach(),
                        1,
                        Integer::sum
                );
            }
        }

        List<Map.Entry<Integer, Double>> sorted =
                coachScores.entrySet()
                        .stream()
                        .map(entry -> Map.entry(
                                entry.getKey(),
                                entry.getValue()
                                        .stream()
                                        .mapToDouble(Double::doubleValue)
                                        .average()
                                        .orElse(0)
                        ))
                        .sorted((a, b) ->
                                Double.compare(b.getValue(), a.getValue()))
                        .toList();

        List<String> result = new ArrayList<>();
        String[] medals = {"🥇", "🥈", "🥉"};

        UserService userService = new UserService();

        for (int i = 0; i < Math.min(3, sorted.size()); i++) {

            int coachId = sorted.get(i).getKey();
            double score = sorted.get(i).getValue();

            UserApp coach = userService.getById(coachId);

            // 🔒 SÉCURITÉ ANTI-CRASH
            if (coach == null) {
                System.out.println("Coach introuvable ID = " + coachId);
                continue; // on ignore ce coach
            }

            int nbSeances =
                    coachSessionCount.getOrDefault(coachId, 0);

            String ligne =
                    medals[i] + " "
                            + coach.getNom() + " " + coach.getPrenom()
                            + " — "
                            + Math.round(score) + "% "
                            + "(" + nbSeances + " séance"
                            + (nbSeances > 1 ? "s" : "")
                            + ")";

            result.add(ligne);
        }

        return result;
    }
    public void updateSeanceToTermineeIfNeeded(Seance s) throws SQLException {

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime endDateTime =
                LocalDateTime.of(
                        s.getDateSeance(),
                        s.getHeureFin()
                );

        if (s.getStatutSeance() == StatutSeance.PLANIFIEE
                && !now.isBefore(endDateTime)) {

            String sql = """
            UPDATE seance
            SET statut_seance = ?
            WHERE id_seance = ?
        """;

            PreparedStatement ps = conx.prepareStatement(sql);
            ps.setString(1, StatutSeance.TERMINEE.name());
            ps.setInt(2, s.getIdSeance());
            ps.executeUpdate();
        }
    }
    public List<Seance> getByIds(Set<Integer> ids) throws SQLException {

        if (ids == null || ids.isEmpty())
            return List.of();

        List<Seance> list = new ArrayList<>();

        String inSql = ids.stream()
                .map(id -> "?")
                .collect(java.util.stream.Collectors.joining(","));

        String sql = "SELECT * FROM seance WHERE id_seance IN (" + inSql + ")";

        PreparedStatement ps = conx.prepareStatement(sql);

        int index = 1;
        for (Integer id : ids) {
            ps.setInt(index++, id);
        }

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
            s.setIdPlanning(rs.getInt("id_planning"));
            s.setStatutSeance(
                    StatutSeance.valueOf(rs.getString("statut_seance"))
            );

            list.add(s);
        }

        return list;
    }
    public List<Seance> getMostReservedSeances() throws SQLException {

        String sql = """
        SELECT s.*, COUNT(r.id_seance) as total
        FROM seance s
        JOIN reservation_seance r ON s.id_seance = r.id_seance
        WHERE r.statut = 'CONFIRMEE'
        GROUP BY s.id_seance
        ORDER BY total DESC
        LIMIT 5
    """;

        List<Seance> list = new ArrayList<>();

        Statement st = conx.createStatement();
        ResultSet rs = st.executeQuery(sql);

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
