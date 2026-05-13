package Services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import Entities.Inscription;
import Entities.Pack;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitaire pour valider que les inscriptions sont correctement sauvegardées
 * en base de données avec l'AUTO_INCREMENT généré.
 */
public class InscriptionServiceFixTest {

    private InscriptionService inscriptionService;
    private PackService packService;
    private Pack testPack;
    
    @BeforeEach
    public void setUp() {
        inscriptionService = new InscriptionService();
        packService = new PackService();
        
        // Préparer un pack de test
        testPack = new Pack();
        testPack.setIdPack(1); // Assurez-vous qu'un pack avec l'ID 1 existe
        testPack.setNom("Pack Test");
        testPack.setPrixBase(new BigDecimal("100.00"));
        testPack.setReduction(new BigDecimal("10.00"));
        testPack.setNbActivitesMax(5);
    }

    /**
     * TEST 1: Vérifier que add() retourne un ID valide
     */
    @Test
    public void testAddInscriptionReturnsValidId() {
        Inscription insc = new Inscription();
        insc.setIdUser(1);
        insc.setIdPack(1);
        insc.setDateInscription(LocalDateTime.now());
        insc.setStatutInscr("EN_ATTENTE");
        insc.setMontantTotal(new BigDecimal("90.00"));
        insc.setNomUser("Test User");
        insc.setNomPack("Pack Test");
        
        // Exécuter la sauvegarde
        int inscriptionId = inscriptionService.add(insc, testPack);
        
        // Vérifications
        assertTrue(inscriptionId > 0, "L'ID retourné doit être > 0");
        assertEquals(inscriptionId, insc.getIdInscription(), "L'objet doit avoir l'ID défini");
        System.out.println("✅ Test 1 PASSED: ID généré = " + inscriptionId);
    }

    /**
     * TEST 2: Vérifier que l'inscription peut être récupérée après insertion
     */
    @Test
    public void testInscriptionCanBeRetrievedAfterInsert() {
        Inscription insc = new Inscription();
        insc.setIdUser(1);
        insc.setIdPack(1);
        insc.setDateInscription(LocalDateTime.now());
        insc.setStatutInscr("EN_ATTENTE");
        insc.setMontantTotal(new BigDecimal("90.00"));
        insc.setNomUser("Retrieve Test User");
        insc.setNomPack("Pack Test");
        
        // Insérer
        int inscriptionId = inscriptionService.add(insc, testPack);
        assertTrue(inscriptionId > 0, "L'inscription devrait être créée");
        
        // Récupérer
        Inscription retrieved = inscriptionService.getById(inscriptionId);
        
        // Vérifier
        assertNotNull(retrieved, "L'inscription doit pouvoir être retrouvée");
        assertEquals(inscriptionId, retrieved.getIdInscription(), "L'ID doit correspondre");
        assertEquals("Retrieve Test User", retrieved.getNomUser(), "Le nom doit correspondre");
        System.out.println("✅ Test 2 PASSED: Inscription trouvée en BD avec les bonnes données");
    }

    /**
     * TEST 3: Vérifier que les IDs auto-générés sont uniques
     */
    @Test
    public void testGeneratedIdsAreUnique() {
        int id1 = createTestInscription("User 1");
        int id2 = createTestInscription("User 2");
        int id3 = createTestInscription("User 3");
        
        assertTrue(id1 > 0 && id2 > 0 && id3 > 0, "Tous les IDs doivent être valides");
        assertNotEquals(id1, id2, "Les IDs doivent être différents");
        assertNotEquals(id2, id3, "Les IDs doivent être différents");
        assertTrue(id2 > id1 && id3 > id2, "Les IDs doivent être croissants");
        System.out.println("✅ Test 3 PASSED: IDs uniques et croissants: " + id1 + ", " + id2 + ", " + id3);
    }

    /**
     * TEST 4: Vérifier que les montants sont correctement sauvegardés
     */
    @Test
    public void testMoneyAmountsAreSavedCorrectly() {
        Inscription insc = new Inscription();
        insc.setIdUser(1);
        insc.setIdPack(1);
        insc.setDateInscription(LocalDateTime.now());
        insc.setStatutInscr("EN_ATTENTE");
        insc.setMontantTotal(new BigDecimal("123.45"));
        insc.setNomUser("Money Test User");
        insc.setNomPack("Pack Test");
        
        int inscriptionId = inscriptionService.add(insc, testPack);
        Inscription retrieved = inscriptionService.getById(inscriptionId);
        
        assertNotNull(retrieved, "L'inscription doit exister");
        assertEquals(new BigDecimal("123.45").setScale(2, java.math.RoundingMode.HALF_UP), 
                     retrieved.getMontantTotal(), "Le montant doit être correct");
        System.out.println("✅ Test 4 PASSED: Montant sauvegardé correctement: " + retrieved.getMontantTotal());
    }

    // ============================================================
    // MÉTHODE HELPER
    // ============================================================

    private int createTestInscription(String userName) {
        Inscription insc = new Inscription();
        insc.setIdUser(1);
        insc.setIdPack(1);
        insc.setDateInscription(LocalDateTime.now());
        insc.setStatutInscr("EN_ATTENTE");
        insc.setMontantTotal(new BigDecimal("100.00"));
        insc.setNomUser(userName);
        insc.setNomPack("Pack Test");
        
        return inscriptionService.add(insc, testPack);
    }

    // ============================================================
    // NOTES
    // ============================================================
    
    /*
     * POUR EXÉCUTER CES TESTS:
     * 
     * 1. Placer ce fichier dans: src/test/java/Services/
     * 2. Assurez-vous que JUnit 5 est configuré dans pom.xml:
     *    <dependency>
     *        <groupId>org.junit.jupiter</groupId>
     *        <artifactId>junit-jupiter-api</artifactId>
     *        <version>5.9.0</version>
     *        <scope>test</scope>
     *    </dependency>
     * 
     * 3. Exécuter: mvn test -Dtest=InscriptionServiceFixTest
     * 
     * 4. Vérifiez les logs:
     *    ✅ Test 1 PASSED
     *    ✅ Test 2 PASSED
     *    ✅ Test 3 PASSED
     *    ✅ Test 4 PASSED
     */
}
