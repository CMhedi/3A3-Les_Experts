package Services;

import Entities.Conversation;
import Entities.UserApp;
import enums.RoleUser;

public class MessagingAccessManager {

    public boolean isAdmin(UserApp user) {
        return user != null && user.getRole() == RoleUser.ADMIN;
    }

    public boolean canOpenMessenger(UserApp user) {
        return user != null;
    }

    public boolean canAccessConversation(UserApp user, Conversation conversation, int creatorId, boolean isParticipant) {
        if (user == null || conversation == null) {
            return false;
        }

        if (isAdmin(user)) {
            return true;
        }

        return isParticipant || user.getIdUser() == creatorId;
    }

    public String resolveMessengerMode(UserApp user) {
        if (isAdmin(user)) {
            return "admin";
        }

        return "user";
    }
}
