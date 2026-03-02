package model.analytics;

public class UserProfile {

    private int userId;
    private int favoriteCoach;
    private String preferredTime;

    public UserProfile(int userId, int favoriteCoach, String preferredTime) {
        this.userId = userId;
        this.favoriteCoach = favoriteCoach;
        this.preferredTime = preferredTime;
    }

    public int getUserId() {
        return userId;
    }

    public int getFavoriteCoach() {
        return favoriteCoach;
    }

    public String getPreferredTime() {
        return preferredTime;
    }
}