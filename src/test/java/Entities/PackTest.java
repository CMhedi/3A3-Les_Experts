package Entities;

import enums.StatutPack;
import enums.TypePack;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class PackTest {

    @Test
    void shouldSetAndGetFields() {
        Pack p = new Pack();

        p.setIdPack(1);
        p.setNom("Starter");
        p.setTypePack(TypePack.INDIVIDUEL);
        p.setPrixBase(new BigDecimal("120.50"));
        p.setReduction(new BigDecimal("10.00"));
        p.setNbActivitesMax(5);
        p.setStatutPack(StatutPack.ACTIF);

        assertEquals(1, p.getIdPack());
        assertEquals("Starter", p.getNom());
        assertEquals(TypePack.INDIVIDUEL, p.getTypePack());
        assertEquals(new BigDecimal("120.50"), p.getPrixBase());
        assertEquals(new BigDecimal("10.00"), p.getReduction());
        assertEquals(5, p.getNbActivitesMax());
        assertEquals(StatutPack.ACTIF, p.getStatutPack());
    }

    @Test
    void toStringShouldContainMainInfo() {
        Pack p = new Pack();
        p.setNom("Premium");
        p.setTypePack(TypePack.ENTREPRISE);
        p.setStatutPack(StatutPack.INACTIF);

        String s = p.toString();
        assertTrue(s.contains("Premium"));
        assertTrue(s.contains("ENTREPRISE"));
        assertTrue(s.contains("INACTIF"));
    }
}
