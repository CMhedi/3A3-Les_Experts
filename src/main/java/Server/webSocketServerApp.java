package Server;

import Utiles.StompClientHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import me.friwi.jcefmaven.CefAppBuilder;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// ============================================================
// ✅ SINGLE SERVER: WebSocket (STOMP) + LiveKit Token
//    Runs on http://localhost:8080
//    Token endpoint: GET http://localhost:8080/livekit/token
//    WebSocket:      ws://localhost:8080/ws
// ============================================================

@SpringBootApplication
class WebSocketServerApp {
    public static void main(String[] args) {
        SpringApplication.run(WebSocketServerApp.class, args);
        System.out.println(
                "\n╔════════════════════════════════════════╗" +
                        "\n║  ✅ Server Started on port 8080        ║" +
                        "\n║  WebSocket:  ws://localhost:8080/ws   ║" +
                        "\n║  Token:      /livekit/token           ║" +
                        "\n╚════════════════════════════════════════╝\n"
        );
    }
}



// ── WebSocket / STOMP config ─────────────────────────────────
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}

// ── STOMP Call Signal Controller ─────────────────────────────
@Controller
class CallSignalController {

    @MessageMapping("call.start")
    @SendTo("/topic/calls")
    public String handleCallStart(String payload) {
        System.out.println("📡 CALL START: " + payload);
        return payload;
    }

    @MessageMapping("call.accept")
    @SendTo("/topic/calls")
    public String handleCallAccept(String payload) {
        System.out.println("✅ CALL ACCEPT: " + payload);
        return payload;
    }

    @MessageMapping("call.reject")
    @SendTo("/topic/calls")
    public String handleCallReject(String payload) {
        System.out.println("❌ CALL REJECT: " + payload);
        return payload;
    }

    @MessageMapping("call.hangup")
    @SendTo("/topic/calls")
    public String handleHangup(String payload) {  // ✅ enlève @Payload
        System.out.println("📴 HANGUP: " + payload);
        return payload;
    }

    // ✅ AJOUTE CE QUI MANQUAIT
    @MessageMapping("call.cancel")
    @SendTo("/topic/calls")
    public String handleCallCancel(String payload) {
        System.out.println("🚫 CALL CANCEL: " + payload);
        return payload;
    }
}
// ── LiveKit Token REST Controller ────────────────────────────

  public class webSocketServerApp {

    // ─── Constantes ──────────────────────────────────────────────────────────
    private static final String LIVEKIT_URL    = "ws://127.0.0.1:7880";
    private static final String TOKEN_ENDPOINT = "http://127.0.0.1:8080/livekit/token";
    private static final boolean USE_JCEF = true; // true = intégré, false = navigateur externe

    // Pour partager JCEF entre plusieurs instances d'appel
    private static boolean jcefInitialized = false;
    private static CefApp staticCefApp;
    public static CefClient staticCefClient;

    // ─── Composants FXML ────────────────────────────────────────────────────
    @FXML
    private StackPane callHost;
    @FXML private Label titleLabel;
    @FXML private Label     subLabel;
    @FXML private Button muteButton;
    @FXML private Button    cameraButton;

    // ─── Données de l'appel ─────────────────────────────────────────────────
    private Stage stage;
    private int     conversationId;
    private int     currentUserId;
    private String  roomName;
    private boolean micEnabled = true;
    private boolean camEnabled = false;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper om   = new ObjectMapper();

    // JCEF fields (par instance)
    private CefClient  cefClient;
    private CefBrowser cefBrowser;
    private SwingNode swingNode;
    private boolean    jcefReady = false;

    private StompClientHandler stompHandler;

    // ─────────────────────────────────────────────────────────────────────────

    public void init(Stage stage, int conversationId, int currentUserId,
                     boolean videoEnabled, String roomName, StompClientHandler stompHandler) {

        this.stage          = stage;
        this.conversationId = conversationId;
        this.currentUserId  = currentUserId;
        this.stompHandler   = stompHandler;
        this.camEnabled     = videoEnabled;
        this.roomName       = (roomName == null || roomName.isBlank())
                ? "conv-" + conversationId : roomName;

        if (titleLabel  != null) titleLabel.setText("📞 " + this.roomName);
        if (subLabel    != null) subLabel.setText("Ouverture de l'appel...");

        fetchToken(this.roomName, "user-" + currentUserId, "user-" + currentUserId)
                .thenAccept(token -> Platform.runLater(() -> {
                    if (USE_JCEF) {
                        new Thread(() -> {
                            try {
                                initJcef();
                                Platform.runLater(() -> {
                                    String html = buildCallHtml(LIVEKIT_URL, token, micEnabled, camEnabled);
                                    String url = startLocalServerAndGetUrl(html);  // ← changement ici
                                    if (url != null) {
                                        waitForJcefAndLoad(url);
                                        if (subLabel != null) subLabel.setText("✅ Appel intégré (serveur local)");
                                    } else {
                                        if (subLabel != null) subLabel.setText("❌ Erreur serveur local");
                                    }
                                });
                            } catch (Exception e) {
                                Platform.runLater(() -> {
                                    if (subLabel != null) subLabel.setText("❌ Erreur JCEF : " + e.getMessage());
                                });
                                e.printStackTrace();
                            }
                        }).start();
                    } else {
                        openCallInBrowser(token);
                        if (subLabel != null) subLabel.setText("✅ Appel ouvert dans le navigateur");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        if (subLabel != null) subLabel.setText("❌ " + ex.getMessage());
                    });
                    return null;
                });

        stage.setOnHidden(ev -> cleanup());
    }

    // ─── Ouverture dans le navigateur externe (fallback) ────────────────────
    private void openCallInBrowser(String token) {
        try {
            String html = buildCallHtml(LIVEKIT_URL, token, micEnabled, camEnabled);

            File tmpDir = new File(System.getenv("TEMP"), "livekit_call");
            tmpDir.mkdirs();
            File htmlFile = new File(tmpDir, "call_" + conversationId + ".html");

            try (PrintWriter pw = new PrintWriter(htmlFile, "UTF-8")) {
                pw.print(html);
            }

            Desktop.getDesktop().browse(htmlFile.toURI());
            System.out.println("✅ Call opened in browser: " + htmlFile.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("❌ Could not open browser: " + e.getMessage());
            if (subLabel != null)
                subLabel.setText("❌ Erreur: " + e.getMessage());
        }
    }

    // ─── Initialisation de JCEF (une seule fois) ────────────────────────────


    private void initJcef() throws Exception {
        if (jcefInitialized) {
            cefClient = staticCefClient;
            cefBrowser = cefClient.createBrowser("about:blank", false, false);
            return;
        }

        CefAppBuilder builder = new CefAppBuilder();
        builder.setInstallDir(new File(
                System.getProperty("user.home") + File.separator + "jcef-bundle"
        ));
        builder.addJcefArgs("--disable-gpu");
        builder.addJcefArgs("--disable-gpu-compositing");
        builder.addJcefArgs("--disable-software-rasterizer");
        builder.addJcefArgs("--enable-media-stream");
        builder.addJcefArgs("--use-fake-device-for-media-stream");
        String cheminVersVideo = "";

        builder.addJcefArgs("--allow-running-insecure-content");
        builder.addJcefArgs("--autoplay-policy=no-user-gesture-required");
        builder.addJcefArgs("--unsafely-treat-insecure-origin-as-secure=data:");
        builder.getCefSettings().windowless_rendering_enabled = false;

        staticCefApp = builder.build();
        staticCefClient = staticCefApp.createClient();
        cefClient = staticCefClient;
        cefBrowser = cefClient.createBrowser("about:blank", false, false);
        cefBrowser.getDevTools().createScreenshot(true);
        jcefInitialized = true;

        Platform.runLater(() -> {
            swingNode = new SwingNode();
            if (callHost != null) callHost.getChildren().setAll(swingNode);

            SwingUtilities.invokeLater(() -> {
                Component comp = cefBrowser.getUIComponent();
                JPanel panel = new JPanel(new BorderLayout());
                panel.add(comp, BorderLayout.CENTER);
                swingNode.setContent(panel);
                jcefReady = true;
            });
        });

        Thread.sleep(800);
    }




    private HttpServer localServer;
    private String startLocalServerAndGetUrl(String html) {
        try {
            int port = 8081 + (int)(Math.random() * 1000);
            System.out.println("🖥️ Démarrage serveur local sur le port " + port);
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/call", exchange -> {
                byte[] response = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.setExecutor(null);
            server.start();
            this.localServer = server;
            String url = "http://localhost:" + port + "/call";
            System.out.println("✅ Serveur local accessible à : " + url);
            return url;
        } catch (Exception e) {
            System.err.println("❌ Échec du serveur local : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    // ─── Attendre que JCEF soit prêt puis charger l'URL ──────────────────────
    private void waitForJcefAndLoad(String url) {
        new Thread(() -> {
            int attempts = 0;
            while (!jcefReady && attempts < 20) {
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                attempts++;
            }
            if (cefBrowser != null) {
                SwingUtilities.invokeLater(() -> cefBrowser.loadURL(url));
            }
        }).start();
    }

    // ─── Fetch token depuis le serveur Spring ───────────────────────────────
    private CompletableFuture<String> fetchToken(String room, String identity, String name) {
        try {
            String url = TOKEN_ENDPOINT
                    + "?room="     + URLEncoder.encode(room,     StandardCharsets.UTF_8)
                    + "&identity=" + URLEncoder.encode(identity, StandardCharsets.UTF_8)
                    + "&name="     + URLEncoder.encode(name,     StandardCharsets.UTF_8);

            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
            return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .thenApply(resp -> {
                        if (resp.statusCode() != 200)
                            throw new RuntimeException("HTTP " + resp.statusCode());
                        try {
                            JsonNode node = om.readTree(resp.body());
                            return node.get("token").asText();
                        } catch (Exception e) {
                            throw new RuntimeException("Bad JSON: " + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            CompletableFuture<String> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    // ─── Contrôles de l'interface ───────────────────────────────────────────
    @FXML
    private void toggleMute() {
        micEnabled = !micEnabled;
        execJs("window.__toggleMic && window.__toggleMic()");
        if (muteButton != null)
            muteButton.setText(micEnabled ? "🎙️ Mute" : "🔇 Unmute");
        if (subLabel != null)
            subLabel.setText(micEnabled ? "Micro activé" : "Micro coupé");
    }

    @FXML
    private void toggleCamera() {
        camEnabled = !camEnabled;
        execJs("window.__toggleCam && window.__toggleCam()");
        if (cameraButton != null)
            cameraButton.setText(camEnabled ? "📷 Camera ON" : "📷 Camera OFF");
        if (subLabel != null)
            subLabel.setText(camEnabled ? "Caméra activée" : "Caméra désactivée");
    }

    @FXML
    private void hangup() {
        sendHangupSignal();
        if (stage != null) stage.close();
    }

    private void sendHangupSignal() {
        if (stompHandler == null || !stompHandler.isConnected()) return;
        try {
            ObjectMapper om = new ObjectMapper();
            String payload = om.writeValueAsString(Map.of(
                    "action",         "HANGUP",
                    "conversationId", conversationId,
                    "senderId",       currentUserId
            ));
            stompHandler.send("/app/call.hangup", payload.getBytes());
            System.out.println("📴 HANGUP signal sent");
        } catch (Exception e) {
            System.err.println("⚠️ HANGUP error: " + e.getMessage());
        }
    }

    private void execJs(String js) {
        try {
            if (cefBrowser != null)
                SwingUtilities.invokeLater(() ->
                        cefBrowser.executeJavaScript(js, cefBrowser.getURL(), 0));
        } catch (Exception ignored) {}
    }

    private void cleanup() {

        if (localServer != null) {
            localServer.stop(0);
            localServer = null;
        }
        sendHangupSignal();
        try {
            File tmpDir  = new File(System.getenv("TEMP"), "livekit_call");
            File htmlFile = new File(tmpDir, "call_" + conversationId + ".html");
            if (htmlFile.exists()) htmlFile.delete();
        } catch (Exception ignored) {}
    }

    // ─── Génération de la page HTML LiveKit (inchangée) ─────────────────────
    private static String toDataUrl(String html) {
        String b64 = Base64.getEncoder()
                .encodeToString(html.getBytes(StandardCharsets.UTF_8));
        return "data:text/html;base64," + b64;
    }

    private static String buildCallHtml(String livekitUrl, String token,
                                        boolean micOn, boolean camOn) {
        String html = "<!doctype html><html><head><meta charset='utf-8'/>"
                + "<style>"
                + "* { box-sizing:border-box; margin:0; padding:0; }"
                + "html,body { height:100%; background:#0b0f14; color:#fff; font-family:system-ui,sans-serif; }"
                + "#grid { display:grid; grid-template-columns:repeat(auto-fit,minmax(260px,1fr)); gap:12px; padding:12px; height:calc(100vh - 60px); }"
                + ".tile { position:relative; background:#111827; border-radius:16px; overflow:hidden; min-height:180px; }"
                + "video { width:100%; height:100%; object-fit:cover; display:block; background:#000; }"
                + ".avatar { width:100%; height:100%; display:flex; align-items:center; justify-content:center; font-size:60px; background:#1f2937; }"
                + ".name { position:absolute; left:10px; bottom:10px; background:rgba(0,0,0,.6); padding:4px 10px; border-radius:999px; font-size:12px; }"
                + "#status { position:fixed; top:10px; left:50%; transform:translateX(-50%); background:rgba(0,0,0,.75); padding:6px 18px; border-radius:999px; font-size:13px; z-index:99; }"
                + "#bar { position:fixed; bottom:0; left:0; right:0; height:60px; background:#111827; display:flex; align-items:center; justify-content:center; gap:16px; }"
                + "button { padding:10px 22px; border:none; border-radius:999px; cursor:pointer; font-size:13px; font-weight:600; }"
                + ".btn-mute  { background:#374151; color:white; }"
                + ".btn-cam   { background:#374151; color:white; }"
                + ".btn-hang  { background:#ef4444; color:white; }"
                + "</style></head><body>"
                + "<div id='status'>Connexion...</div>"
                + "<div id='grid'></div>"
                + "<script type='module'>"
                + "import { Room, RoomEvent, Track } from 'https://unpkg.com/livekit-client@2/dist/livekit-client.esm.mjs';"
                + "const LIVEKIT_URL = '_LK_';"
                + "const TOKEN = '_TK_';"
                + "let state = { mic: _MIC_, cam: _CAM_ };"
                + "const grid = document.getElementById('grid');"
                + "const status = document.getElementById('status');"
                + "const room = new Room();"
                + "function setStatus(msg) { status.textContent = msg; }"
                + "function ensureTile(pid, name) {"
                + "  let t = grid.querySelector('.tile[data-pid=\"'+pid+'\"]');"
                + "  if (t) return t;"
                + "  t = document.createElement('div'); t.className='tile'; t.dataset.pid=pid;"
                + "  const av = document.createElement('div'); av.className='avatar'; av.textContent='👤';"
                + "  const lb = document.createElement('div'); lb.className='name'; lb.textContent=name||pid;"
                + "  t.appendChild(av); t.appendChild(lb); grid.appendChild(t); return t;"
                + "}"
                + "function attachTrack(participant, track, isLocal) {"
                + "  const pid  = isLocal ? 'me' : participant.identity;"
                + "  const nm   = participant.name || participant.identity || pid;"
                + "  const tile = ensureTile(pid, isLocal ? nm+' (vous)' : nm);"
                + "  if (track.kind === Track.Kind.Video) {"
                + "    let v = tile.querySelector('video');"
                + "    if (!v) { v = document.createElement('video'); v.autoplay=true; v.playsInline=true; v.muted=isLocal; tile.prepend(v); }"
                + "    track.attach(v);"
                + "  }"
                + "}"
                + "room.on(RoomEvent.ParticipantConnected, p => {"
                + "  setStatus(p.identity+' a rejoint'); ensureTile(p.identity, p.name||p.identity);"
                + "  p.trackPublications.forEach(pub => { if(pub.track) attachTrack(p, pub.track, false); });"
                + "});"
                + "room.on(RoomEvent.TrackSubscribed, (track,_,p) => attachTrack(p, track, false));"
                + "room.on(RoomEvent.ParticipantDisconnected, p => {"
                + "  const t = grid.querySelector('.tile[data-pid=\"'+p.identity+'\"]'); if(t) t.remove();"
                + "  setStatus(p.identity+' a quitté');"
                + "});"
                + "room.on(RoomEvent.Disconnected, () => setStatus('Appel terminé'));"
                + "async function join() {"
                + "  setStatus('Connexion...');"
                + "  await room.connect(LIVEKIT_URL, TOKEN);"
                + "  setStatus('Connecté ✅');"
                + "  await room.localParticipant.setMicrophoneEnabled(state.mic);"
                + "  await room.localParticipant.setCameraEnabled(state.cam);"
                + "  const me = room.localParticipant;"
                + "  ensureTile('me', (me.name||me.identity||'Vous')+' (vous)');"
                + "  me.trackPublications.forEach(pub => { if(pub.track) attachTrack(me, pub.track, true); });"
                + "  window.__toggleMic = async () => { state.mic=!state.mic; await room.localParticipant.setMicrophoneEnabled(state.mic); setStatus(state.mic?'Micro activé':'Micro coupé'); return state.mic; };"
                + "  window.__toggleCam = async () => { state.cam=!state.cam; await room.localParticipant.setCameraEnabled(state.cam); me.trackPublications.forEach(pub=>{ if(pub.track) attachTrack(me,pub.track,true); }); setStatus(state.cam?'Caméra ON':'Caméra OFF'); return state.cam; };"
                + "  window.__hangup = () => { try{room.disconnect();}catch(e){} setStatus('Appel terminé'); };"
                + "}"
                + "join().catch(err => { console.error(err); setStatus('Erreur: '+err.message); });"
                + "</script></body></html>";

        return html
                .replace("_LK_", escJs(livekitUrl))
                .replace("_TK_", escJs(token))
                .replace("_MIC_", micOn ? "true" : "false")
                .replace("_CAM_", camOn ? "true" : "false");
    }

    private static String escJs(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("'","\\'").replace("\"","\\\"").replace("\n","\\n");
    }
}