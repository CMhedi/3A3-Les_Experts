package Services.interfaces;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class GifService {

    private final String apiKey;

    public GifService(String apiKey) {
        this.apiKey = apiKey;
    }

    public JSONArray searchGifs(String query, int limit) {

        try {

            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

            String urlString =
                    "https://api.giphy.com/v1/gifs/search?api_key="
                            + apiKey +
                            "&q=" + encodedQuery +
                            "&limit=" + limit;

            URL url = new URL(urlString);
            HttpURLConnection conn =
                    (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.connect();

            Scanner sc = new Scanner(url.openStream());
            StringBuilder sb = new StringBuilder();

            while (sc.hasNext()) {
                sb.append(sc.nextLine());
            }

            sc.close();

            JSONObject json = new JSONObject(sb.toString());
            JSONArray data = json.getJSONArray("data");

            JSONArray urls = new JSONArray();

            for (int i = 0; i < data.length(); i++) {

                String gifUrl = data.getJSONObject(i)
                        .getJSONObject("images")
                        .getJSONObject("fixed_height")
                        .getString("url");

                JSONObject obj = new JSONObject();
                obj.put("url", gifUrl);
                urls.put(obj);
            }

            System.out.println("Found gifs: " + urls.length());

            return urls;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new JSONArray();
    }
}