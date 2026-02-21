package Tests;

import Models.Activite;
import Services.ActiviteService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ActiviteServiceTest {

    @Test
    void crudActivite() {
        ActiviteService service = new ActiviteService();

        // create-
        Activite a = new Activite(
                0,
                "Test Activite JUnit",
                "SPORT",
                "RUNNING",
                "DEBUTANT",
                25.50,
                "DISPONIBLE",
                null
        );

        int newId = service.addAndReturnId(a);
        assertTrue(newId > 0);
        System.out.println("✅ Inserted activite id=" + newId);

        // read
        Activite fromDb = service.getById(newId);
        assertNotNull(fromDb);
        assertEquals("Test Activite JUnit", fromDb.getNom());

        // read
        assertTrue(service.getAll().stream().anyMatch(x -> x.getIdActivite() == newId));

        // maj
        Activite updated = new Activite(
                newId,
                "Test Activite Updated",
                "CULTUREL",
                "YOGA",
                "INTERMEDIAIRE",
                40.00,
                "INDISPONIBLE",
                "https://example.com/img.png"
        );

        assertDoesNotThrow(() -> service.update(updated));

        Activite afterUpdate = service.getById(newId);
        assertNotNull(afterUpdate);
        assertEquals("Test Activite Updated", afterUpdate.getNom());
        assertEquals("CULTUREL", afterUpdate.getTypeActivite());
        assertEquals("YOGA", afterUpdate.getCategorieAct());
        assertEquals("INTERMEDIAIRE", afterUpdate.getNiveauAct());
        assertEquals("INDISPONIBLE", afterUpdate.getStatut());

        // ---------- DELETE ----------
        assertDoesNotThrow(() -> service.delete(newId));
        assertNull(service.getById(newId));

        System.out.println("✅ CRUD OK for activite id=" + newId);
    }
}
