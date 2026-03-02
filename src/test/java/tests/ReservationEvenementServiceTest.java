package tests;

import Entities.Evenement;
import Entities.ReservationEvenement;
import Services.EvenementService;
import Services.ReservationEvenementService;
import enums.CategorieEvenement;
import enums.StatutReservation;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReservationEvenementServiceTest {

    private static ReservationEvenementService service;
    private static EvenementService evenementService;

    private static int idEvenementSupport;
    private static int idReservation;

    private static final String EVT_MARK = "UT_EVT_SUPPORT_";

    @BeforeAll
    static void setup() throws Exception {
        service = new ReservationEvenementService();
        evenementService = new EvenementService();

        Evenement e = new Evenement();
        e.setTitre(EVT_MARK + System.currentTimeMillis());
        e.setDescription("Event support pour tests reservation");
        e.setCategorieEvt(CategorieEvenement.values()[0]);
        e.setDateEvent(LocalDateTime.now().plusDays(5));
        e.setLieu("Tunis");
        e.setNbPlaces(200);
        e.setStatut("DISPONIBLE");
        e.setImageUrl("https://img.test/support.png");

        evenementService.add(e);

        List<Evenement> all = evenementService.getAll();
        Evenement inserted = all.stream()
                .filter(x -> x.getTitre() != null && x.getTitre().startsWith(EVT_MARK))
                .max(Comparator.comparingInt(Evenement::getIdEvenement))
                .orElse(null);

        assertNotNull(inserted);
        idEvenementSupport = inserted.getIdEvenement();
        assertTrue(idEvenementSupport > 0);
    }

    @Test
    @Order(1)
    void add_reservation() throws Exception {
        ReservationEvenement r = new ReservationEvenement();
        r.setDateReservation(LocalDateTime.now());
        r.setStatutRes(StatutReservation.values()[0]);
        r.setNbBillets(2);
        r.setIdEvenement(idEvenementSupport);

        service.add(r);

        List<ReservationEvenement> all = service.getAll();
        assertNotNull(all);
        assertFalse(all.isEmpty());

        ReservationEvenement inserted = all.stream()
                .filter(x -> x.getIdEvenement() == idEvenementSupport && x.getNbBillets() == 2)
                .max(Comparator.comparingInt(ReservationEvenement::getIdResEvt))
                .orElse(null);

        assertNotNull(inserted);
        idReservation = inserted.getIdResEvt();
        assertTrue(idReservation > 0);
    }

    @Test
    @Order(2)
    void getById_reservation() throws Exception {
        assertTrue(idReservation > 0);
        ReservationEvenement r = service.getById(idReservation);
        assertNotNull(r);
        assertEquals(idReservation, r.getIdResEvt());
    }

    @Test
    @Order(3)
    void update_reservation() throws Exception {
        assertTrue(idReservation > 0);

        ReservationEvenement r = service.getById(idReservation);
        assertNotNull(r);

        r.setNbBillets(5);
        r.setStatutRes(StatutReservation.values()[0]);
        r.setDateReservation(LocalDateTime.now().plusHours(2));

        service.update(r);

        ReservationEvenement updated = service.getById(idReservation);
        assertNotNull(updated);
        assertEquals(5, updated.getNbBillets());
        assertEquals(idEvenementSupport, updated.getIdEvenement());
    }

    @Test
    @Order(4)
    void delete_reservation() throws Exception {
        assertTrue(idReservation > 0);
        service.delete(idReservation);

        ReservationEvenement deleted = service.getById(idReservation);
        assertNull(deleted);
        idReservation = 0;
    }

    @AfterAll
    static void teardown() throws Exception {
        if (idReservation > 0) {
            try {
                service.delete(idReservation);
            } catch (Exception ignored) {
            }
        }

        if (idEvenementSupport > 0) {
            try {
                evenementService.delete(idEvenementSupport);
            } catch (Exception ignored) {
            }
        }
    }
}
