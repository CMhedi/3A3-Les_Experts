package Services;

import Services.PdfExportService;
import Services.QrCodeService;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.ImageIO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

public class QrPdfApiServer {

    private static String env(String k, String fallback) {
        String v = System.getenv(k);
        return (v == null || v.trim().isEmpty()) ? fallback : v.trim();
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(env("EA_QR_API_PORT", "8086"));
        String publicBaseUrl = env("EA_PUBLIC_BASE_URL", "=http://192.168.1.114:" + port);

        PdfExportService pdfService = new PdfExportService();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // 1) QR generator
        server.createContext("/api/qr", ex -> {
            try {
                Map<String, String> q = query(ex);
                String kind = q.getOrDefault("kind", "both").toLowerCase();
                String pdfUrl = publicBaseUrl + "/api/export/pdf?kind=" + kind;

                byte[] png = QrCodeService.generatePng(pdfUrl, 320);
                ex.getResponseHeaders().set("Content-Type", "image/png");
                ex.sendResponseHeaders(200, png.length);
                try (OutputStream os = ex.getResponseBody()) { os.write(png); }
            } catch (Exception e) {
                sendText(ex, 400, "QR error: " + e.getMessage());
            }
        });

        // 2) PDF download (this is what phone opens after scan)
        server.createContext("/api/export/pdf", ex -> {
            try {
                Map<String, String> q = query(ex);
                String kindRaw = q.getOrDefault("kind", "both").toUpperCase(Locale.ROOT);

                PdfExportService.Kind kind = PdfExportService.Kind.BOTH;
                if ("PACKS".equals(kindRaw) || "PACK".equals(kindRaw)) kind = PdfExportService.Kind.PACKS;
                else if ("INSCRIPTIONS".equals(kindRaw) || "INSCRIPTION".equals(kindRaw)) kind = PdfExportService.Kind.INSCRIPTIONS;

                byte[] pdf = pdfService.buildPdf(kind);

                String filename = "EcoAdventure_" + kind + "_" + LocalDate.now() + ".pdf";
                ex.getResponseHeaders().set("Content-Type", "application/pdf");
                ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                ex.sendResponseHeaders(200, pdf.length);
                try (OutputStream os = ex.getResponseBody()) { os.write(pdf); }
            } catch (Exception e) {
                sendText(ex, 500, "PDF error: " + e.getMessage());
            }
        });

        // 3) Optional: Scan API (send QR image -> returns decoded text)
        server.createContext("/api/scan", ex -> {
            try {
                if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                    sendText(ex, 405, "Use POST with image/png or image/jpeg body");
                    return;
                }
                byte[] bytes = ex.getRequestBody().readAllBytes();
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                if (img == null) {
                    sendText(ex, 400, "Invalid image");
                    return;
                }

                LuminanceSource source = new BufferedImageLuminanceSource(img);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                Result result = new MultiFormatReader().decode(bitmap);

                sendText(ex, 200, result.getText());
            } catch (Exception e) {
                sendText(ex, 400, "Scan error: " + e.getMessage());
            }
        });

        server.start();
        System.out.println("✅ QR/PDF API running: http://localhost:" + port);
        System.out.println("✅ Set EA_PUBLIC_BASE_URL for phone access.");
    }

    private static Map<String, String> query(HttpExchange ex) throws Exception {
        String raw = ex.getRequestURI().getRawQuery();
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isBlank()) return map;

        for (String p : raw.split("&")) {
            int i = p.indexOf('=');
            if (i < 0) continue;
            String k = URLDecoder.decode(p.substring(0, i), "UTF-8");
            String v = URLDecoder.decode(p.substring(i + 1), "UTF-8");
            map.put(k, v);
        }
        return map;
    }

    private static void sendText(HttpExchange ex, int code, String text) throws IOException {
        byte[] b = text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }
}