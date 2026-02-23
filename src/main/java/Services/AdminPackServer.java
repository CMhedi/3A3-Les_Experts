package Services;

import Entities.Pack;
import enums.StatutPack;
import enums.TypePack;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AdminPackServer {

    private final PackService service = new PackService();

    public static void main(String[] args) throws Exception {
        int port = 8085; // ✅ بدّلها كيف تحب
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        AdminPackServer app = new AdminPackServer();

        server.createContext("/admin", app::serveAdminPage);
        server.createContext("/admin/packs/create", app::handleCreate);

        // (اختياري) update/delete
        // server.createContext("/admin/packs/update", app::handleUpdate);
        // server.createContext("/admin/packs/delete", app::handleDelete);

        server.setExecutor(null);
        server.start();
        System.out.println("✅ AdminPackServer running on http://localhost:" + port + "/admin");
    }

    private void serveAdminPage(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            sendText(ex, 405, "Method Not Allowed");
            return;
        }

        // ✅ نفس HTML اللي فوق (مضمّن هنا)
        String html = loadResourceOrInline();
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(200, html.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(html.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void handleCreate(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            sendText(ex, 405, "Method Not Allowed");
            return;
        }

        try {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> form = parseForm(body);

            Pack p = new Pack();
            p.setNom(req(form, "nom"));
            p.setTypePack(TypePack.valueOf(req(form, "type").toUpperCase()));
            p.setPrixBase(new BigDecimal(req(form, "prixBase").replace(',', '.')));

            String redRaw = form.getOrDefault("reduction", "").trim();
            p.setReduction(redRaw.isEmpty() ? BigDecimal.ZERO : new BigDecimal(redRaw.replace(',', '.')));

            p.setNbActivitesMax(Integer.parseInt(req(form, "max")));
            p.setStatutPack(StatutPack.valueOf(req(form, "statut").toUpperCase()));

            // ✅ INSERT DB
            service.add(p);

            // ✅ ردّ بسيط للـ admin بعد الارسال
            sendHtml(ex, 200,
                    "<h2>✅ Pack ajouté</h2><p>Tu peux fermer la page.</p><p><a href=\"/admin\">Ajouter un autre</a></p>");

        } catch (Exception e) {
            e.printStackTrace();
            sendHtml(ex, 400, "<h2>❌ Erreur</h2><pre>" + escape(e.getMessage()) + "</pre><p><a href=\"/admin\">Retour</a></p>");
        }
    }

    // ---------------- helpers ----------------

    private static Map<String, String> parseForm(String body) throws UnsupportedEncodingException {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.isBlank()) return map;
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx < 0) continue;
            String k = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
            String v = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
            map.put(k, v);
        }
        return map;
    }

    private static String req(Map<String, String> map, String key) {
        String v = map.get(key);
        if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException("Champ manquant: " + key);
        return v.trim();
    }

    private static void sendText(HttpExchange ex, int code, String text) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        byte[] b = text.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    private static void sendHtml(HttpExchange ex, int code, String html) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        byte[] b = html.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    private static String loadResourceOrInline() {
        // Inline HTML minimal (نفس الفورم)
        return """
        <!doctype html><html lang="fr"><head>
        <meta charset="utf-8"/><meta name="viewport" content="width=device-width,initial-scale=1"/>
        <title>EcoAdventure — Admin Pack</title>
        <style>
          body{font-family:system-ui,Arial;margin:0;background:#0b0f10;color:#fff}
          .wrap{max-width:520px;margin:0 auto;padding:18px}
          .card{background:rgba(255,255,255,.06);border:1px solid rgba(255,255,255,.12);border-radius:16px;padding:16px}
          h1{font-size:18px;margin:0 0 12px}
          label{display:block;font-size:13px;opacity:.85;margin-top:10px}
          input,select{width:100%;padding:12px;border-radius:12px;border:1px solid rgba(255,255,255,.14);background:rgba(0,0,0,.25);color:#fff}
          button{width:100%;margin-top:14px;padding:12px;border-radius:12px;border:0;background:#5fe98c;color:#06210f;font-weight:700}
          .hint{opacity:.7;font-size:12px;margin-top:10px}
        </style></head><body>
        <div class="wrap"><div class="card">
          <h1>Créer un Pack</h1>
          <form method="post" action="/admin/packs/create">
            <label>Nom</label><input name="nom" required placeholder="Pack Été 2026"/>
            <label>Type</label>
            <select name="type" required>
              <option value="BASIC">GROUPE</option>
              <option value="SILVER">SILVER</option>
              <option value="GOLD">GOLD</option>
            </select>
            <label>Prix de base</label><input name="prixBase" required placeholder="50.00"/>
            <label>Réduction</label><input name="reduction" placeholder="5.00"/>
            <label>Nb activités max</label><input name="max" required placeholder="10"/>
            <label>Statut</label>
            <select name="statut" required>
              <option value="ACTIF">ACTIF</option>
              <option value="INACTIF">INACTIF</option>
            </select>
            <button type="submit">Envoyer</button>
            <div class="hint">Après envoi, le pack sera ajouté dans la DB et apparaîtra dans l’app au refresh.</div>
          </form>
        </div></div></body></html>
        """;
    }
}