package Tests;

import Models.Reservation;
import Services.ReservationService;
import org.junit.jupiter.api.Test;
import utils.DataBase;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Date;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ReservationServiceTest {

    private int getAnyId(String table, String idColumn) throws Exception {
        Connection cnx = DataBase.getInstance().getConx();
        String sql = "SELECT " + idColumn + " FROM " + table + " LIMIT 1";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }

        throw new RuntimeException("No rows found in table: " + table);
    }

    @Test
    void testCrudreservation() throws Exception {
        ReservationService service = new ReservationService();

        int userId = getAnyId("user_app", "id_user");
        int activiteId = getAnyId("activite", "id_activite");

        //  create
        Reservation r = new Reservation(
                0,
                Date.valueOf(LocalDate.now().plusDays(2)),
                "EN_ATTENTE",
                3,
                userId,
                activiteId
        );

        int newId = service.addAndReturnId(r);
        assertTrue(newId > 0);

        // le read par id
        Reservation fromDb = service.getById(newId);
        assertNotNull(fromDb);

        // If your getter is getId() not getIdReservation(), change this:
        assertEquals(newId, fromDb.getId());

        // ---------- READ (getAll contains it) ----------
        assertTrue(service.getAll().stream().anyMatch(x -> x.getId() == newId));

        // update
        Reservation updated = new Reservation(
                newId,
                Date.valueOf(LocalDate.now().plusDays(5)),
                "CONFIRMEE",
                5,
                userId,
                activiteId
        );

        service.update(updated);

        Reservation afterUpdate = service.getById(newId);
        assertNotNull(afterUpdate);
        assertEquals("CONFIRMEE", afterUpdate.getStatut());
        assertEquals(5, afterUpdate.getNbPersonnes());

        // supprimer
        service.delete(newId);

        Reservation afterDelete = service.getById(newId);
        assertNull(afterDelete);
    }
}
