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

                JSONObject gifObj = data.getJSONObject(i);
                JSONObject images = gifObj.getJSONObject("images");

                // ✅ استخدم الصورة الثابتة بدل الـ GIF المتحركة
                // البدائل المتاحة:
                // 1. fixed_height_still (صورة ثابتة)
                // 2. fixed_height_small_still (صورة صغيرة ثابتة)
                // 3. preview (معاينة)

                String stillImageUrl;

                try {
                    // محاول 1: الصورة الثابتة (الأفضل)
                    stillImageUrl = images.getJSONObject("fixed_height_still")
                            .getString("url");
                } catch (Exception e1) {
                    try {
                        // محاول 2: الصورة الصغيرة الثابتة
                        stillImageUrl = images.getJSONObject("fixed_height_small_still")
                                .getString("url");
                    } catch (Exception e2) {
                        try {
                            // محاول 3: صورة الـ GIF الأصلية (كحد أخير)
                            stillImageUrl = images.getJSONObject("fixed_height")
                                    .getString("url");
                        } catch (Exception e3) {
                            // تخطي هذا الـ GIF إذا فشل
                            System.err.println("⚠️ لم تتمكن من الحصول على صورة للـ GIF #" + i);
                            continue;
                        }
                    }
                }

                JSONObject obj = new JSONObject();
                obj.put("url", stillImageUrl);
                urls.put(obj);

                System.out.println("✅ صورة #" + (urls.length()) + " محملة بنجاح");
            }

            System.out.println("✅ عدد الصور المحملة: " + urls.length());

            return urls;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new JSONArray();
    }
}