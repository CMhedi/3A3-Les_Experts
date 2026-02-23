import Entities.Planning;
import Services.PlanningService;
import exceptions.ValidationException;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PlanningServiceTest {

    private static PlanningService planningService;
    private static int planningId;

    @BeforeAll
    static void setup() {
        planningService = new PlanningService();
    }

    // ==================================================
    // ADD
    // ==================================================
    @Test
    @Order(1)
    void ajouterPlanningTest() throws SQLException {

        Planning p = new Planning();
        p.setPeriode("JUnit Planning");
        p.setDescription("Test ajout planning");

        planningService.add(p);

        List<Planning> plannings = planningService.getAll();

        Planning created = plannings.stream()
                .filter(pl -> pl.getPeriode().equals("JUnit Planning"))
                .findFirst()
                .orElse(null);

        assertNotNull(created);
        assertFalse(plannings.isEmpty());

        planningId = created.getIdPlanning();
    }

    // ==================================================
    // GET BY ID
    // ==================================================
    @Test
    @Order(2)
    void getPlanningByIdTest() throws SQLException {

        Planning p = planningService.getById(planningId);

        assertNotNull(p);
        assertEquals("JUnit Planning", p.getPeriode());
    }

    // ==================================================
    // UPDATE
    // ==================================================
    @Test
    @Order(3)
    void modifierPlanningTest() throws SQLException {

        Planning existing = planningService.getById(planningId);
        assertNotNull(existing);

        existing.setPeriode("JUnit Planning Modifié");
        existing.setDescription("Description modifiée");

        planningService.update(existing);

        Planning updated = planningService.getById(planningId);

        assertEquals("JUnit Planning Modifié", updated.getPeriode());
        assertEquals("Description modifiée", updated.getDescription());
    }

    // ==================================================
    // GET ALL
    // ==================================================
    @Test
    @Order(4)
    void getAllPlanningTest() throws SQLException {

        List<Planning> plannings = planningService.getAll();

        assertNotNull(plannings);
        assertFalse(plannings.isEmpty());
    }

    // ==================================================
    // VALIDATION
    // ==================================================
    @Test
    @Order(5)
    void validationPlanningTest() {

        Planning p = new Planning();
        p.setPeriode(""); // invalide

        assertThrows(ValidationException.class,
                () -> planningService.add(p));
    }

    // ==================================================
    // DELETE
    // ==================================================
    @Test
    @Order(6)
    void deletePlanningTest() throws SQLException {

        planningService.delete(planningId);

        Planning deleted = planningService.getById(planningId);

        assertNull(deleted);
    }
}
