package test.java.Services;

import Entities.UserApp;
import Services.UserService;
import enums.RoleUser;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.List;

// 1. Définition de l'ordre d'exécution [cite: 186, 188]
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserTest {
    static UserService us = new UserService();
    static int idUserTest; // Bechi n-sajlou l'ID mta3 el user mta3 el test

    // 2. Initialisation (Optionnel mais recommandé) [cite: 184, 187]
    @BeforeAll
    static void setup() {
        // Code d'initialisation si nécessaire
    }

    // 3. Test d'ajout [cite: 166, 192, 196]
    @Test
    @Order(1)
    void testAjoutUser() throws SQLException {
        UserApp u = new UserApp();
        u.setNom("TestNom");
        u.setPrenom("TestPrenom");
        u.setEmail("test@esprit.tn");
        u.setRole(RoleUser.USER_SIMPLE);
        u.setMotDePasse("123456");

        us.add(u);

        List<UserApp> users = us.getAll();
        assertFalse(users.isEmpty()); // Thabbet el list mouch fergha [cite: 196]

        // Thabbet el user t-zed s7i7 [cite: 197]
        boolean trouve = users.stream().anyMatch(user -> user.getEmail().equals("test@esprit.tn"));
        assertTrue(trouve);
    }

    // 4. Test de suppression (Bech el base to93od ndhifa) [cite: 169, 211]
    @Test
    @Order(2)
    void testSupprimerUser() throws SQLException {
        // Lawaj 3la el user elli zedneh bech n-fasskhouh
        UserApp userToDelete = us.getAll().stream()
                .filter(u -> u.getEmail().equals("test@esprit.tn"))
                .findFirst()
                .orElse(null);

        assertNotNull(userToDelete); // Lezem nal9awh [cite: 182]

        us.delete(userToDelete.getIdUser()); // Fassakh

        // Thabbet ma3adech mawjoud [cite: 211]
        boolean existe = us.getAll().stream().anyMatch(u -> u.getIdUser() == userToDelete.getIdUser());
        assertFalse(existe);
    }

    // 5. Nettoyage automatique après chaque test [cite: 212, 214]
    @AfterEach
    void cleanUp() throws SQLException {
        // Hna t-najem t-zid logic bech t-nadhaf el base ken thamma 7aja okhra
        // "Un bon test ne laisse aucune trace" [cite: 216]
    }
}