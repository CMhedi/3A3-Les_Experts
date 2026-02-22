package Services.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TranslationService {
    private static final String ACCOUNT_ID = "72b45b34-58a1-4db6-b6a9-b9f376eb236a";
    private static final String API_KEY = "AIzaSyA1FJz30cyJGcplg-hB5r1GKRXgQ1wOifI";
    private static final String BASE_URL = "https://smartcat.ai/api/integration/v1";
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String authHeader;

    public TranslationService() {
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
        String auth = ACCOUNT_ID + ":" + API_KEY;
        String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        this.authHeader = "Basic " + encoded;
    }

    /**
     * ✅ CORRECTED: Crée un projet SANS VENDEUR LIÉ
     * Utilise la traduction manuelle (pas d'assignation automatique)
     * Format multipart/form-data exigé par l'API Smartcat.
     *
     * NOTE: Si vous voulez la traduction automatique, vous DEVEZ:
     * 1. Aller sur https://smartcat.ai/user/settings/integrations
     * 2. Lier un vendeur (Google, DeepL, OpenAI, etc.)
     * 3. Puis utiliser la méthode createProjectWithVendor()
     */
    public String createProject(int conversationId, String targetLanguage) throws Exception {
        String url = BASE_URL + "/project/create";

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", "Chat Conversation " + conversationId + " (" + System.currentTimeMillis() + ")");
        metadata.put("sourceLanguage", "fr");
        metadata.put("targetLanguages", List.of(targetLanguage));

        // ✅ SOLUTION: Ne pas assigner à un vendeur
        // Cela crée un projet vide attendant une traduction manuelle
        metadata.put("assignToVendor", false);

        System.out.println("🔧 Creating project for language: " + targetLanguage + " (without auto-vendor assignment)");

        String metadataJson = mapper.writeValueAsString(metadata);
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        byte[] multipartBody = buildMetadataOnlyMultipart(metadataJson, boundary);

        int maxRetries = 3;
        int attempt = 0;
        while (attempt < maxRetries) {
            attempt++;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("🔸 Create project response status: " + response.statusCode() + " (attempt " + attempt + "/" + maxRetries + ")");
            System.out.println("🔸 Response body: " + response.body());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                JsonNode root = mapper.readTree(response.body());
                String projectId;
                if (root.isArray()) {
                    projectId = root.get(0).get("id").asText();
                } else {
                    projectId = root.get("id").asText();
                }
                System.out.println("✅ Project created successfully: " + projectId);
                return projectId;
            } else if (response.statusCode() == 500 && attempt < maxRetries) {
                System.out.println("⚠️ Server error (500), retrying in 2 seconds...");
                Thread.sleep(2000);
                continue;
            } else if (response.statusCode() == 400) {
                System.out.println("❌ Bad Request (400) - Project creation failed");
                throw new RuntimeException("Échec création projet: 400 - " + response.body());
            } else {
                throw new RuntimeException("Échec création projet: " + response.statusCode() + " - " + response.body());
            }
        }
        throw new RuntimeException("Failed to create project after " + maxRetries + " attempts");
    }

    /**
     * ⚡ ALTERNATIVE: Crée un projet AVEC VENDEUR LIÉ
     * À utiliser UNIQUEMENT si vous avez lié un vendeur dans Smartcat
     *
     * Pour lier un vendeur:
     * 1. Allez sur https://smartcat.ai/user/settings/integrations
     * 2. Sélectionnez un moteur (Google, DeepL, OpenAI, etc.)
     * 3. Autorisez la connexion
     * 4. Utilisez ensuite cette méthode
     */
    public String createProjectWithVendor(int conversationId, String targetLanguage, String vendorId) throws Exception {
        String url = BASE_URL + "/project/create";

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", "Chat Conversation " + conversationId + " (" + System.currentTimeMillis() + ")");
        metadata.put("sourceLanguage", "fr");
        metadata.put("targetLanguages", List.of(targetLanguage));

        // ✅ Avec vendeur spécifique
        metadata.put("assignToVendor", true);
        metadata.put("vendorAccountIds", List.of(vendorId)); // Vendor ID fourni en paramètre

        System.out.println("🔧 Creating project with vendor " + vendorId + " for language: " + targetLanguage);

        String metadataJson = mapper.writeValueAsString(metadata);
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        byte[] multipartBody = buildMetadataOnlyMultipart(metadataJson, boundary);

        int maxRetries = 3;
        int attempt = 0;
        while (attempt < maxRetries) {
            attempt++;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("🔸 Create project response status: " + response.statusCode());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                JsonNode root = mapper.readTree(response.body());
                String projectId = root.isArray() ? root.get(0).get("id").asText() : root.get("id").asText();
                System.out.println("✅ Project created with vendor: " + projectId);
                return projectId;
            } else if (response.statusCode() == 500 && attempt < maxRetries) {
                System.out.println("⚠️ Server error (500), retrying...");
                Thread.sleep(2000);
            } else {
                System.out.println("❌ Error: " + response.body());
                throw new RuntimeException("Failed to create project: " + response.statusCode());
            }
        }
        throw new RuntimeException("Failed to create project after " + maxRetries + " attempts");
    }

    /**
     * Upload un document dans le projet.
     */
    public String uploadDocument(String projectId, String messageText, String fileName) throws Exception {
        if (projectId == null || projectId.isEmpty()) {
            throw new RuntimeException("Cannot upload document: projectId is null or empty");
        }

        String url = BASE_URL + "/project/document?projectId=" + projectId;
        System.out.println("📤 Uploading " + fileName + " (" + messageText.length() + " bytes) to project " + projectId);

        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        byte[] multipartBody = buildFileOnlyMultipart(messageText.getBytes(StandardCharsets.UTF_8), boundary, fileName);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", authHeader)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("🔸 Upload document response status: " + response.statusCode());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            JsonNode root = mapper.readTree(response.body());
            String docId;
            if (root.isArray() && root.size() > 0) {
                docId = root.get(0).get("id").asText();
            } else if (root.has("id")) {
                docId = root.get("id").asText();
            } else {
                docId = root.get(0).get("id").asText();
            }
            System.out.println("✅ Document uploaded successfully: " + docId);
            return docId;
        } else {
            throw new RuntimeException("Échec upload document: " + response.statusCode() + " - " + response.body());
        }
    }

    /**
     * Démarre le workflow du projet (traduction).
     */
    /**
     * Démarre le workflow du projet (traduction).
     */
    public void startProject(String projectId) throws Exception {
        // ✅ Correction: utiliser le format avec paramètre query, cohérent avec completeProject
        String url = BASE_URL + "/project/start?projectId=" + projectId;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", authHeader)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 204) {
            throw new RuntimeException("Échec démarrage projet: " + response.statusCode() + " - " + response.body());
        }
        System.out.println("✅ Projet démarré avec succès");
    }

    /**
     * Attend qu'un document soit complètement traduit (statut = completed).
     * @return true si le document est prêt, false sinon
     */
    public boolean waitForDocumentCompletion(String documentId, int maxAttempts, long initialDelay) throws Exception {
        String url = BASE_URL + "/document/" + documentId;
        int attempt = 0;
        long delay = initialDelay;

        while (attempt < maxAttempts) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", authHeader)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String body = response.body();
                System.out.println("📄 Document status body: " + body);
                JsonNode root = mapper.readTree(body);
                String status = root.get("status").asText();
                System.out.println("🔍 Document status: " + status + " (attempt " + (attempt + 1) + "/" + maxAttempts + ")");
                if ("completed".equalsIgnoreCase(status) || "ready".equalsIgnoreCase(status)) {
                    return true;
                }
            } else {
                System.out.println("⚠️ Document check returned " + response.statusCode());
            }

            Thread.sleep(delay);
            delay = Math.min(delay * 2, 10000);
            attempt++;
        }
        return false;
    }

    /**
     * Marque le projet comme complété.
     */
    public void completeProject(String projectId) throws Exception {
        String url = BASE_URL + "/project/complete?projectId=" + projectId;
        int maxAttempts = 5;
        int attempt = 0;

        while (attempt < maxAttempts) {
            attempt++;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", authHeader)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("✅ Complete project status: " + response.statusCode());

            if (response.statusCode() == 200 || response.statusCode() == 204) {
                System.out.println("✅ Project completed successfully");
                return;
            } else if (response.statusCode() == 409 && attempt < maxAttempts) {
                System.out.println("⚠️ Documents not ready (409), waiting 3 seconds...");
                Thread.sleep(3000);
            } else {
                throw new RuntimeException("Failed to complete project: " + response.statusCode());
            }
        }
        throw new RuntimeException("Project completion failed after " + maxAttempts + " attempts");
    }

    /**
     * Télécharge le document traduit.
     */
    public String downloadDocument(String documentId) throws Exception {
        String url = BASE_URL + "/document/" + documentId + "/export";
        int maxAttempts = 10;
        int attempt = 0;
        long waitTime = 1000;

        while (attempt < maxAttempts) {
            attempt++;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", authHeader)
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            System.out.println("📥 Download response status: " + response.statusCode());

            if (response.statusCode() == 200) {
                String result = new String(response.body(), StandardCharsets.UTF_8);
                System.out.println("✅ Document downloaded successfully");
                return result;
            } else if (response.statusCode() == 404 && attempt < maxAttempts) {
                System.out.println("⏳ Document not ready (404), waiting...");
                Thread.sleep(waitTime);
                waitTime = Math.min(waitTime * 2, 5000);
            } else {
                throw new RuntimeException("Download failed: " + response.statusCode());
            }
        }
        throw new RuntimeException("Document download timeout");
    }

    // ========== Utility methods ==========

    private byte[] buildMetadataOnlyMultipart(String metadataJson, String boundary) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String CRLF = "\r\n";

        baos.write(("--" + boundary).getBytes(StandardCharsets.UTF_8));
        baos.write(CRLF.getBytes(StandardCharsets.UTF_8));
        baos.write("Content-Disposition: form-data; name=\"metadata\"".getBytes(StandardCharsets.UTF_8));
        baos.write(CRLF.getBytes(StandardCharsets.UTF_8));
        baos.write("Content-Type: application/json".getBytes(StandardCharsets.UTF_8));
        baos.write(CRLF.getBytes(StandardCharsets.UTF_8));
        baos.write(CRLF.getBytes(StandardCharsets.UTF_8));
        baos.write(metadataJson.getBytes(StandardCharsets.UTF_8));
        baos.write(CRLF.getBytes(StandardCharsets.UTF_8));
        baos.write(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
        baos.write(CRLF.getBytes(StandardCharsets.UTF_8));

        return baos.toByteArray();
    }

    private byte[] buildFileOnlyMultipart(byte[] fileContent, String boundary, String fileName) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String CRLF = "\r\n";

        baos.write(("--" + boundary).getBytes(StandardCharsets.UTF_8));
        baos.write(CRLF.getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"")
                .getBytes(StandardCharsets.UTF_8));
        baos.write(CRLF.getBytes(StandardCharsets.UTF_8));
        baos.write("Content-Type: text/plain; charset=utf-8".getBytes(StandardCharsets.UTF_8));
        baos.write(CRLF.getBytes(StandardCharsets.UTF_8));
        baos.write(CRLF.getBytes(StandardCharsets.UTF_8));
        baos.write(fileContent);
        baos.write(CRLF.getBytes(StandardCharsets.UTF_8));
        baos.write(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
        baos.write(CRLF.getBytes(StandardCharsets.UTF_8));

        return baos.toByteArray();
    }

    public void listMTEngines() throws IOException, InterruptedException {
        String url = BASE_URL + "/account/mtengines";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", authHeader)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("📋 MT Engines: " + response.body());
    }
}