package Utiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SmsService {
    // ✅ Zid kelmet "App " 9bal el Key
    private static final String API_KEY = "App c00f2d45881a009bfd50e65297ded56e-40954b0d-db0b-4721-8182-0f84441997a0";

    // ✅ Zid https:// w el path /sms/2/text/advanced
    private static final String BASE_URL = "https://vyverv.api.infobip.com/sms/2/text/advanced";

    public static void sendOTP(String toPhone, String code) {
        try {
            // 1. Nadhfou el noumrou (Lezem yebda b 216)
            String cleanPhone = toPhone.replace("+", "").replace(" ", "");
            if (!cleanPhone.startsWith("216")) {
                cleanPhone = "216" + cleanPhone;
            }

            // 2. El JSON (Format mrigel)
            String json = "{\"messages\":[{\"destinations\":[{\"to\":\"" + cleanPhone + "\"}],\"from\":\"EcoAdventure\",\"text\":\"Votre code est: " + code + "\"}]}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL))
                    .header("Authorization", API_KEY) // M-rakka7 b kelmet App
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Infobip traja3 200 wala 202 ki yabda mrigel
            if (response.statusCode() == 200 || response.statusCode() == 202) {
                System.out.println("✅ SMS Infobip envoyé: " + response.body());
            } else {
                System.out.println("❌ Erreur Infobip Status: " + response.statusCode());
                System.out.println("Détails: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi du SMS");
            e.printStackTrace();
        }
    }
}