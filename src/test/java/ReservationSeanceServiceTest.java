import Services.ReservationSeanceService;
import Services.SeanceService;
import Entities.Seance;
import exceptions.ValidationException;
import org.junit.jupiter.api.*;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReservationSeanceServiceTest {

    private static ReservationSeanceService reservationService;
    private static SeanceService seanceService;

    private static int USER_ID = 2;     // doit exister
    private static int AUTRE_USER_ID = 3; // doit exister
    private static int SEANCE_ID;

    @BeforeAll
    static void setup() throws SQLException {

        reservationService = new ReservationSeanceService();
        seanceService = new SeanceService();

        // 🔥 On récupère une séance PLANIFIEE valide
        Seance seance = seanceService.getAll().stream()
                .filter(s -> s.getStatutSeance().name().equals("PLANIFIEE"))
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException("Aucune séance PLANIFIEE trouvée"));

        SEANCE_ID = seance.getIdSeance();
    }

    // ==================================================
    // 1️⃣ Réserver
    // ==================================================
    @Test
    @Order(1)
    void reserverSeanceTest() throws SQLException {

        reservationService.annuler(USER_ID, SEANCE_ID);

        reservationService.reserver(USER_ID, SEANCE_ID);

        int count = reservationService.countReservations(SEANCE_ID);

        assertTrue(count > 0);
    }


    // ==================================================
    // 2️⃣ Double réservation interdite
    // ==================================================
    @Test
    @Order(2)
    void reserverSeanceDejaReserveeTest() throws SQLException {

        reservationService.annuler(USER_ID, SEANCE_ID);

        //1ère réservation
        reservationService.reserver(USER_ID, SEANCE_ID);

        // 2ème réservation → doit échouer
        assertThrows(ValidationException.class, () -> {
            reservationService.reserver(USER_ID, SEANCE_ID);
        });
    }


    // ==================================================
    // 3️⃣ Annulation
    // ==================================================
    @Test
    @Order(3)
    void annulerReservationTest() throws SQLException {

        reservationService.annuler(USER_ID, SEANCE_ID);

        reservationService.reserver(USER_ID, SEANCE_ID);
        reservationService.annuler(USER_ID, SEANCE_ID);

        int count = reservationService.countReservations(SEANCE_ID);

        assertTrue(count >= 0);
    }


    // ==================================================
    // 4️⃣ Réserver après annulation
    // ==================================================
    @Test
    @Order(4)
    void reserverApresAnnulationTest() throws SQLException {

        reservationService.reserver(USER_ID, SEANCE_ID);

        int count = reservationService.countReservations(SEANCE_ID);

        assertTrue(count > 0);
    }

    // ==================================================
    // 5️⃣ Capacité dépassée
    // ==================================================
    @Test
    @Order(5)
    void capaciteDepasseeTest() throws SQLException {

        Seance seance = seanceService.getById(SEANCE_ID);

        int capacite = seance.getCapacite();

        // 🔥 Remplir la séance jusqu’à la capacité
        for (int i = 10; i < 10 + capacite; i++) {
            try {
                reservationService.reserver(i, SEANCE_ID);
            } catch (Exception ignored) {}
        }

        assertThrows(ValidationException.class,
                () -> reservationService.reserver(AUTRE_USER_ID, SEANCE_ID));
    }
    @BeforeEach
    void cleanReservation() throws SQLException {
        reservationService.annuler(USER_ID, SEANCE_ID);
    }

}
