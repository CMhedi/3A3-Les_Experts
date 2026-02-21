package Utiles;

import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * ✅ FINAL FIX: STOMP Client Handler with raw frame handling
 * Avoids message converter issues by handling frames directly
 */
public class StompClientHandler {

    private StompSession stompSession;
    private String serverUrl;
    private boolean isConnected = false;
    private int currentUserId;
    private StompSessionConnectCallback connectCallback;
    private IncomingCallHandler incomingCallHandler;

    // Reconnection parameters
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final long RECONNECT_DELAY = 3000;

    /**
     * Constructor - stores server URL and user ID
     * @param serverUrl WebSocket server URL (e.g., "ws://localhost:8080/ws")
     * @param userId    Current user ID
     */
    public StompClientHandler(String serverUrl, int userId) {
        this.serverUrl = serverUrl;
        this.currentUserId = userId;
        System.out.println("🔧 StompClientHandler initialized for User " + userId);
    }

    /**
     * Starts STOMP connection asynchronously
     * @param callback     Called after successful connection
     * @param callHandler  Handles incoming call messages
     */
    public void connect(StompSessionConnectCallback callback, IncomingCallHandler callHandler) {
        this.connectCallback = callback;
        this.incomingCallHandler = callHandler;
        new Thread(this::run, "STOMP-Connection-Thread").start();
    }

    /**
     * Internal connection loop - runs in separate thread
     */
    private void run() {
        try {
            System.out.println("\n🔗 Connecting to STOMP server: " + serverUrl);

            // Create STOMP client with SockJS transport
            WebSocketStompClient stompClient = new WebSocketStompClient(
                    new SockJsClient(
                            Collections.singletonList(
                                    new WebSocketTransport(new StandardWebSocketClient())
                            )
                    )
            );

            // ✅ DO NOT set message converter - we'll handle frames manually
            // This avoids the "No suitable converter" error

            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();

            // Connect with handler
            stompClient.connect(serverUrl, headers, new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    Platform.runLater(() -> {
                        stompSession = session;
                        isConnected = true;
                        reconnectAttempts = 0;

                        System.out.println("\n" +
                                "╔════════════════════════════════════════╗\n" +
                                "║  ✅ STOMP CONNECTED SUCCESSFULLY       ║\n" +
                                "║  Session ID: " + String.format("%-21s", session.getSessionId().substring(0, Math.min(8, session.getSessionId().length()))) + "║\n" +
                                "║  User ID: " + String.format("%-27s", currentUserId) + "║\n" +
                                "╚════════════════════════════════════════╝\n");

                        if (connectCallback != null) {
                            connectCallback.onConnected(session);
                        }

                        // Subscribe to incoming calls
                        subscribeToIncomingCalls();
                    });
                }

                @Override
                public void handleException(StompSession session, StompCommand command,
                                            StompHeaders headers, byte[] payload, Throwable exception) {
                    Platform.runLater(() -> {
                        System.err.println("\n❌ STOMP Exception: " + exception.getMessage());

                        if (exception.getCause() != null) {
                            System.err.println("    Caused by: " + exception.getCause().getMessage());
                        }

                        handleConnectionError();
                    });
                }

                @Override
                public void handleTransportError(StompSession session, Throwable exception) {
                    Platform.runLater(() -> {
                        System.err.println("\n❌ STOMP Transport Error: " + exception.getMessage());
                        isConnected = false;
                        attemptReconnect();
                    });
                }
            });

        } catch (Exception e) {
            Platform.runLater(() -> {
                System.err.println("\n❌ STOMP Initialization Error: " + e.getMessage());
                handleConnectionError();
            });
        }
    }

    /**
     * Subscribe to incoming calls on /topic/calls
     * ✅ Uses raw frame handler to avoid converter issues
     */
    private void subscribeToIncomingCalls() {
        if (stompSession == null || !stompSession.isConnected()) {
            System.err.println("❌ Cannot subscribe - STOMP not connected");
            return;
        }

        try {
            String destination = "/topic/calls";
            System.out.println("📬 Subscribing to: " + destination);

            // ✅ Use raw frame handler with byte[] payload type
            stompSession.subscribe(destination, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    // Return byte[].class to receive raw binary data without conversion
                    return byte[].class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    if (payload == null) {
                        System.out.println("⚠️ Received null payload");
                        return;
                    }

                    try {
                        String message;

                        // ✅ Handle different payload types (though we expect byte[])
                        if (payload instanceof byte[]) {
                            message = new String((byte[]) payload, StandardCharsets.UTF_8);
                        } else if (payload instanceof String) {
                            message = (String) payload;
                        } else {
                            message = payload.toString();
                        }

                        System.out.println("\n" +
                                "╔════════════════════════════════════════╗\n" +
                                "║  📨 RECEIVED CALL SIGNAL              ║\n" +
                                "║  From: /topic/calls                   ║\n" +
                                "║  Length: " + String.format("%-27s", message.length() + " bytes") + "║\n" +
                                "╚════════════════════════════════════════╝");
                        System.out.println("📄 Payload: " + message);

                        if (incomingCallHandler != null) {
                            try {
                                incomingCallHandler.onIncomingCall(message);
                            } catch (Exception e) {
                                System.err.println("❌ Error in call handler: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("❌ Error processing payload: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });

            System.out.println("✅ Successfully subscribed to incoming calls\n");

        } catch (Exception e) {
            System.err.println("❌ Subscription error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send a call signal
     * @param conversationId Conversation ID
     * @param callType       "AUDIO_CALL" or "VIDEO_CALL"
     * @param senderName     Sender's name
     * @param senderId       Sender's user ID
     */
    public void sendCallSignal(int conversationId, String callType, String senderName, int senderId) {
        if (!isConnected || stompSession == null) {
            System.err.println("⚠️ STOMP not connected - cannot send call signal");
            return;
        }

        try {
            String destination = "/app/call.start";

            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("senderName", senderName);
            payloadMap.put("senderId", senderId);
            payloadMap.put("conversationId", conversationId);
            payloadMap.put("type", callType);

            ObjectMapper mapper = new ObjectMapper();
            String jsonPayload = mapper.writeValueAsString(payloadMap);

            System.out.println("\n" +
                    "╔════════════════════════════════════════╗\n" +
                    "║  📤 SENDING CALL SIGNAL                ║\n" +
                    "║  Destination: /app/call.start          ║\n" +
                    "║  Caller: " + String.format("%-25s", senderName) + "║\n" +
                    "╚════════════════════════════════════════╝");
            System.out.println("📄 Payload: " + jsonPayload);

            stompSession.send(destination, jsonPayload.getBytes());

            System.out.println("✅ Call signal sent successfully\n");

        } catch (Exception e) {
            System.err.println("❌ Error sending call signal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generic send method
     * @param destination STOMP destination
     * @param payload     Message payload as bytes
     */
    public void send(String destination, byte[] payload) {
        if (!isConnected || stompSession == null) {
            System.err.println("⚠️ STOMP not connected - cannot send");
            return;
        }

        try {
            System.out.println("📤 Sending to " + destination + " (" + payload.length + " bytes)");
            stompSession.send(destination, payload);
            System.out.println("✅ Message sent to " + destination);

        } catch (Exception e) {
            System.err.println("❌ Error sending message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle connection error
     */
    private void handleConnectionError() {
        isConnected = false;
        System.err.println("❌ Connection error - attempting to reconnect...");
        attemptReconnect();
    }

    /**
     * Attempt reconnection with exponential backoff
     */
    private void attemptReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            System.err.println("\n❌ Max reconnection attempts (" + MAX_RECONNECT_ATTEMPTS + ") reached");
            return;
        }

        reconnectAttempts++;
        long delay = RECONNECT_DELAY * reconnectAttempts;

        System.out.println("🔄 Reconnection attempt " + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS +
                " in " + (delay / 1000) + " seconds...");

        new Thread(() -> {
            try {
                Thread.sleep(delay);
                Platform.runLater(() -> {
                    connect(connectCallback, incomingCallHandler);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "STOMP-Reconnect-Thread").start();
    }

    /**
     * Check if STOMP is connected
     */
    public boolean isConnected() {
        return isConnected && stompSession != null && stompSession.isConnected();
    }

    /**
     * Disconnect STOMP session
     */
    public void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            try {
                stompSession.disconnect();
                isConnected = false;
                System.out.println("✅ STOMP disconnected");
            } catch (Exception e) {
                System.err.println("❌ Error disconnecting: " + e.getMessage());
            }
        }
    }

    /**
     * Callback interface for connection events
     */
    public interface StompSessionConnectCallback {
        void onConnected(StompSession session);
    }

    /**
     * Callback interface for incoming calls
     */
    public interface IncomingCallHandler {
        void onIncomingCall(String messagePayload);
    }
}