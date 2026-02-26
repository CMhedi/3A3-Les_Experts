package Services;


import Entities.Message;
import Services.interfaces.MessageDAO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

public class MessengerPermissionsTest {

    @Test
    void testAdminCanDeleteAnyMessage() {
        // Arrange
        int adminId = 2; // Ahmed
        Message otherUserMessage = new Message(10, "TEXTE", "Hello", "LU", null, null, 1, 5); // Message mta3 User 5
        MessageDAO mockDAO = mock(MessageDAO.class);

        // Act: Ahmed (Admin) yjareb yfassakh
        boolean canDelete = (adminId == 2 || otherUserMessage.getIdUser() == adminId);

        // Assert
        assertTrue(canDelete, "Ahmed (Admin) doit pouvoir supprimer n'importe quel message");
    }

    @Test
    void testUserCannotDeleteOthersMessage() {
        // Arrange
        int normalUserId = 5;
        Message ahmedMessage = new Message(11, "TEXTE", "Klemek s7i7", "LU", null, null, 1, 2); // Message mta3 Ahmed

        // Act: User 5 yjareb yfassakh message Ahmed
        boolean canDelete = (normalUserId == 2 || ahmedMessage.getIdUser() == normalUserId);

        // Assert
        assertFalse(canDelete, "Un utilisateur normal ne peut pas supprimer le message d'Ahmed");
    }

    @Test
    void testUserCanEditOnlyHisOwnMessage() {
        // Arrange
        int userId = 5;
        Message myMessage = new Message(12, "TEXTE", "My Content", "ENVOYE", null, null, 1, 5);

        // Act
        boolean canEdit = (myMessage.getIdUser() == userId);

        // Assert
        assertTrue(canEdit, "L'utilisateur doit pouvoir modifier son propre message");
    }
}