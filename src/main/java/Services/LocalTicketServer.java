package Services;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class LocalTicketServer {

    private static HttpServer server;
    private static final Gson gson = new Gson();

    public static void startOnce() {
        if (server != null) return;

        try {
            TicketService ticketService = new TicketService();
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8090), 0);

            server.createContext("/api/tickets/generate", ex -> {
                try {
                    if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }

                    String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    GenerateReq req = gson.fromJson(body, GenerateReq.class);
                    if (req == null || req.reservationId <= 0) throw new IllegalArgumentException("reservationId invalide");

                    var result = ticketService.generateTicket(req.reservationId);
                    String pngB64 = Base64.getEncoder().encodeToString(result.pngBytes());

                    var resp = new GenerateResp(result.reservationId(), result.token(), pngB64);

                    byte[] out = gson.toJson(resp).getBytes(StandardCharsets.UTF_8);
                    ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                    ex.sendResponseHeaders(200, out.length);
                    try (OutputStream os = ex.getResponseBody()) { os.write(out); }

                } catch (Exception e) {
                    e.printStackTrace();
                    String json = "{\"error\":\"" + e.getMessage().replace("\"","'") + "\"}";
                    byte[] out = json.getBytes(StandardCharsets.UTF_8);
                    ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                    ex.sendResponseHeaders(500, out.length);
                    try (OutputStream os = ex.getResponseBody()) { os.write(out); }
                } finally { ex.close(); }
            });

            server.createContext("/api/tickets/verify", ex -> {
                try {
                    if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }

                    String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    VerifyReq req = gson.fromJson(body, VerifyReq.class);
                    if (req == null || req.token == null || req.token.isBlank()) throw new IllegalArgumentException("token manquant");

                    var result = ticketService.verifyAndCheckIn(req.token);

                    byte[] out = gson.toJson(result).getBytes(StandardCharsets.UTF_8);
                    ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                    ex.sendResponseHeaders(200, out.length);
                    try (OutputStream os = ex.getResponseBody()) { os.write(out); }

                } catch (Exception e) {
                    e.printStackTrace();
                    String json = "{\"error\":\"" + e.getMessage().replace("\"","'") + "\"}";
                    byte[] out = json.getBytes(StandardCharsets.UTF_8);
                    ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                    ex.sendResponseHeaders(400, out.length);
                    try (OutputStream os = ex.getResponseBody()) { os.write(out); }
                } finally { ex.close(); }
            });

            server.setExecutor(null);
            server.start();
            System.out.println("[LocalTicketServer] http://127.0.0.1:8090 started");

        } catch (Exception e) {
            throw new RuntimeException("Erreur démarrage serveur ticket: " + e.getMessage(), e);
        }
    }

    private static class GenerateReq { int reservationId; }
    private static class VerifyReq { String token; }
    private record GenerateResp(int reservationId, String token, String pngBase64) {}
}