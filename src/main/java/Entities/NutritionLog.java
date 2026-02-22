package Entities;

import java.time.LocalDate;

public class NutritionLog {

    private int id;
    private int userId;
    private String foodName;
    private double calories;
    private double protein;
    private double fat;
    private double carbs;
    private LocalDate logDate;

    public NutritionLog(int userId,
                        String foodName,
                        double calories,
                        double protein,
                        double fat,
                        double carbs,
                        LocalDate logDate) {

        this.userId = userId;
        this.foodName = foodName;
        this.calories = calories;
        this.protein = protein;
        this.fat = fat;
        this.carbs = carbs;
        this.logDate = logDate;
    }

    public int getUserId() { return userId; }
    public String getFoodName() { return foodName; }
    public double getCalories() { return calories; }
    public double getProtein() { return protein; }
    public double getFat() { return fat; }
    public double getCarbs() { return carbs; }
    public LocalDate getLogDate() { return logDate; }
}