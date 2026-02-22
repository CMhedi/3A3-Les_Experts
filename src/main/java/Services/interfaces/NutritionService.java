package Services.interfaces;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class NutritionService {

    private static final String API_KEY = "5cdb0a7de4b6405fb5a0e5450eaf6961";

    public static double[] getNutritionValues(String ingredient) throws Exception {

        URL url = new URL("https://api.spoonacular.com/recipes/parseIngredients?apiKey=" + API_KEY);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);

        String postData = "ingredientList=" + ingredient +
                "&servings=1" +
                "&includeNutrition=true";

        OutputStream os = conn.getOutputStream();
        os.write(postData.getBytes());
        os.flush();
        os.close();

        int status = conn.getResponseCode();

        BufferedReader in =
                new BufferedReader(new InputStreamReader(conn.getInputStream()));

        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }

        in.close();

        System.out.println("STATUS: " + status);
        System.out.println("BODY: " + response);

        JSONArray jsonArray = new JSONArray(response.toString());

        JSONObject nutrition = jsonArray
                .getJSONObject(0)
                .getJSONObject("nutrition");

        JSONArray nutrients = nutrition.getJSONArray("nutrients");

        double calories = 0;
        double protein = 0;
        double fat = 0;
        double carbs = 0;

        for (int i = 0; i < nutrients.length(); i++) {

            JSONObject n = nutrients.getJSONObject(i);
            String name = n.getString("name");

            switch (name) {
                case "Calories":
                    calories = n.getDouble("amount");
                    break;
                case "Protein":
                    protein = n.getDouble("amount");
                    break;
                case "Fat":
                    fat = n.getDouble("amount");
                    break;
                case "Carbohydrates":
                    carbs = n.getDouble("amount");
                    break;
            }
        }

        return new double[]{calories, protein, fat, carbs};
    }
}