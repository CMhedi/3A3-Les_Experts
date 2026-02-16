import Entities.Seance;
import Services.interfaces.SeanceService;
import enums.StatutSeance;
import exceptions.ValidationException;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SeanceServiceTest {

    private SeanceService seanceService;
    private int seanceId;

    @BeforeAll
    void setup() {
        seanceService = new SeanceService();
    }

    // ==================================================
    // ADD
    // ==================================================
    @Test
    @Order(1)
    void ajouterSeanceTest() throws SQLException {

        Seance s = new Seance();
        s.setDateSeance(LocalDate.now().plusDays(1));
        s.setHeureDebut(LocalTime.of(9, 0));
        s.setHeureFin(LocalTime.of(10, 0));
        s.setCapacite(15);
        s.setStatutSeance(StatutSeance.PLANIFIEE);

        // ⚠️ doivent exister dans ta base
        s.setIdPlanning(1);
        s.setIdCoach(1);

        seanceService.add(s);

        List<Seance> seances = seanceService.getAll();

        Seance added = seances.stream()
                .filter(se -> se.getCapacite() == 15)
                .findFirst()
                .orElse(null);

        assertNotNull(added);

        seanceId = added.getIdSeance();
    }

    // ==================================================
    // GET BY ID
    // ==================================================
    @Test
    @Order(2)
    void getSeanceByIdTest() throws SQLException {

        Seance s = seanceService.getById(seanceId);

        assertNotNull(s);
        assertEquals(15, s.getCapacite());
    }

    // ==================================================
    // UPDATE
    // ==================================================
    @Test
    @Order(3)
    void modifierSeanceTest() throws SQLException {

        Seance s = seanceService.getById(seanceId);

        s.setCapacite(25);
        s.setStatutSeance(StatutSeance.TERMINEE);

        seanceService.update(s);

        Seance updated = seanceService.getById(seanceId);

        assertEquals(25, updated.getCapacite());
        assertEquals(StatutSeance.TERMINEE, updated.getStatutSeance());
    }

    // ==================================================
    // GET ALL
    // ==================================================
    @Test
    @Order(4)
    void getAllSeanceTest() throws SQLException {

        List<Seance> seances = seanceService.getAll();

        assertNotNull(seances);
        assertTrue(seances.size() > 0);
    }

    // ==================================================
    // DELETE
    // ==================================================
    @Test
    @Order(5)
    void deleteSeanceTest() throws SQLException {

        seanceService.delete(seanceId);

        Seance deleted = seanceService.getById(seanceId);

        assertNull(deleted);
    }

    // ==================================================
    // VALIDATION
    // ==================================================
    @Test
    @Order(6)
    void validationSeanceTest() {

        Seance s = new Seance();
        s.setDateSeance(LocalDate.now());
        s.setHeureDebut(LocalTime.of(11, 0));
        s.setHeureFin(LocalTime.of(10, 0)); // heure Fin < début
        s.setCapacite(-1);
        s.setIdPlanning(0);
        s.setIdCoach(0);

        assertThrows(ValidationException.class,
                () -> seanceService.add(s));
    }
}
