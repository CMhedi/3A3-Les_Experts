package Entities;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class InscriptionTest {

    @Test
    void shouldSetAndGetFields() {
        Inscription i = new Inscription();

        LocalDateTime now = LocalDateTime.now();
        i.setIdInscription(10);
        i.setDateInscription(now);
        i.setStatutInscr("CONFIRME");
        i.setMontantTotal(new BigDecimal("250.00"));
        i.setIdUser(1);
        i.setIdPack(3);

        assertEquals(10, i.getIdInscription());
        assertEquals(now, i.getDateInscription());
        assertEquals("CONFIRME", i.getStatutInscr());
        assertEquals(new BigDecimal("250.00"), i.getMontantTotal());
        assertEquals(1, i.getIdUser());
        assertEquals(3, i.getIdPack());
    }

    @Test
    void toStringShouldContainId() {
        Inscription i = new Inscription();
        i.setIdInscription(77);

        String s = i.toString();
        assertTrue(s.contains("77"));
    }
}
