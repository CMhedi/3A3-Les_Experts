package Entities;

import Entities.UserApp;

public class Session {
    // Static bech tnajem t'accediha mel blayes l-kol sans "new"
    private static UserApp connectedUser;

    public static void setConnectedUser(UserApp user) {
        connectedUser = user;
    }

    public static UserApp getConnectedUser() {
        return connectedUser;
    }

    public static void logout() {
        connectedUser = null;
    }
}