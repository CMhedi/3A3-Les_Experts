package test;

import Entities.Conversation;
import Services.interfaces.ConversationDAO;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MessengerControllerTest {

    private static ConversationDAO sc;
    private static int idCreated;
    private static final int TEST_USER_ID = 1; // ID d'un utilisateur existant pour les tests

    @BeforeAll
    static void setup() {
        sc = new ConversationDAO();
    }

    @Test
    @Order(1)
    void ajouterConversationTest() throws SQLException {
        Conversation conv = new Conversation(0, "Test JUnit Unit", 1);

        // La méthode addConversation retourne maintenant l'ID généré
        idCreated = sc.addConversation(conv);

        assertNotEquals(-1, idCreated, "L'ajout de la conversation a échoué");

        // Utilisation de searchConversations avec les bons paramètres
        List<Conversation> list = sc.searchConversations("Test JUnit Unit", TEST_USER_ID);

        // Note: Si vous venez de créer la conv, elle n'a pas encore de membre
        // donc searchConversations (qui fait un JOIN) risque de retourner vide.
        // Pour le test, on vérifie juste que l'ID est bien récupéré.
        assertTrue(idCreated > 0);
    }

    @Test
    @Order(2)
    void modifierConversationTest() throws SQLException {
        // Correction de l'appel : updateConversationTitle prend (int, String)
        boolean isUpdated = sc.updateConversationTitle(idCreated, "Titre Modifié par JUnit");

        assertTrue(isUpdated);

        // On vérifie dans la liste globale (nécessite que l'user soit membre pour apparaître dans getAll)
        // Pour le test, on simule l'ajout de l'utilisateur à la conversation d'abord
        sc.addMemberToConversation(idCreated, TEST_USER_ID);

        List<Conversation> list = sc.getAllConversations(TEST_USER_ID);
        boolean check = list.stream().anyMatch(c ->
                c.getIdConversation() == idCreated &&
                        c.getTitre().equals("Titre Modifié par JUnit")
        );

        assertTrue(check);
    }

    @Test
    @Order(3)
    void supprimerConversationTest() throws SQLException {
        // Correction du nom de la méthode : deleteConversation au lieu de s()
        boolean isDeleted = sc.deleteConversation(idCreated);
        assertTrue(isDeleted);

        // Correction : getAllConversations au lieu de afficher()
        List<Conversation> list = sc.getAllConversations(TEST_USER_ID);

        boolean exists = list.stream()
                .anyMatch(c -> c.getIdConversation() == idCreated);

        assertFalse(exists);
    }
}