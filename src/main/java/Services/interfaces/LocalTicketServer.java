package Services.interfaces;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class LocalTicketServer {

    private static HttpServer server;
    private static final Gson gson = new Gson();

    private static final String HOST = "127.0.0.1";
    private static int port = 8090;                 // start port
    private static final int MAX_PORT_TRIES = 10;   // 8090..8099

    public static synchronized void startOnce() {
        if (server != null) return;

        try {
            TicketService ticketService = new TicketService();

            server = bindServerWithFallback();

            // ---------------- GENERATE ----------------
            server.createContext("/api/tickets/generate", ex -> {
                try {
                    if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                        ex.sendResponseHeaders(405, -1);
                        return;
                    }

                    String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    GenerateReq req = gson.fromJson(body, GenerateReq.class);
                    if (req == null || req.reservationId <= 0)
                        throw new IllegalArgumentException("reservationId invalide");

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
                } finally {
                    ex.close();
                }
            });

            // ---------------- VERIFY (DETAILS) ----------------
            server.createContext("/api/tickets/verify", ex -> {
                try {
                    if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                        ex.sendResponseHeaders(405, -1);
                        return;
                    }

                    String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    VerifyReq req = gson.fromJson(body, VerifyReq.class);
                    if (req == null || req.token == null || req.token.isBlank())
                        throw new IllegalArgumentException("token manquant");

                    var result = ticketService.verifyAndCheckInWithDetails(req.token);

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
                } finally {
                    ex.close();
                }
            });

            server.setExecutor(null);
            server.start();
            System.out.println("[LocalTicketServer] started " + getBaseUrl());

        } catch (Exception e) {
            throw new RuntimeException("Erreur démarrage serveur ticket: " + e.getMessage(), e);
        }
    }

    private static HttpServer bindServerWithFallback() throws Exception {
        int start = port;

        for (int p = start; p < start + MAX_PORT_TRIES; p++) {
            try {
                HttpServer s = HttpServer.create(new InetSocketAddress(HOST, p), 0);
                port = p;
                return s;
            } catch (Exception ex) {
                if (ex instanceof BindException || (ex.getCause() instanceof BindException)) {
                    continue;
                }
                throw ex;
            }
        }

        throw new RuntimeException("Aucun port libre entre " + start + " et " + (start + MAX_PORT_TRIES - 1));
    }

    public static String getBaseUrl() {
        return "http://" + HOST + ":" + port;
    }

    public static int getPort() {
        return port;
    }

    // ----- req/resp -----
    private static class GenerateReq { int reservationId; }
    private static class VerifyReq { String token; }
    private record GenerateResp(int reservationId, String token, String pngBase64) {}
}