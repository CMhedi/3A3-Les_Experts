package Services;

import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class TicketApiClient {

    private final HttpClient http = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public GenerateResp generate(int reservationId) throws Exception {
        String json = gson.toJson(new GenerateReq(reservationId));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:8090/api/tickets/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new RuntimeException(res.body());

        return gson.fromJson(res.body(), GenerateResp.class);
    }

    public VerifyResp verify(String token) throws Exception {
        String json = gson.toJson(new VerifyReq(token));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:8090/api/tickets/verify"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new RuntimeException(res.body());

        return gson.fromJson(res.body(), VerifyResp.class);
    }

    // ✅ utiliser class au lieu de record pour éviter soucis modules/reflection
    public static class GenerateReq {
        public int reservationId;
        public GenerateReq(int reservationId){ this.reservationId = reservationId; }
    }

    public static class VerifyReq {
        public String token;
        public VerifyReq(String token){ this.token = token; }
    }

    public static class GenerateResp {
        public int reservationId;
        public String token;
        public String pngBase64;
    }

    public static class VerifyResp {
        public boolean valid;
        public boolean alreadyUsed;
        public ReservationDetails details;
    }

    public static class ReservationDetails {
        public int reservationId;
        public String statut;
        public int nbPersonnes;
        public int userId;
        public int activiteId;

        public String activiteNom;
        public String typeActivite;
        public String categorie;
        public String niveau;

        public double prixUnitaire;
        public double total;

        public boolean checkedIn;
        public String checkinTime;
    }
}