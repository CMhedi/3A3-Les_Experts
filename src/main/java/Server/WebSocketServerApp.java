package Server;

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

@SpringBootApplication
public class WebSocketServerApp {
    public static void main(String[] args) {
        SpringApplication.run(WebSocketServerApp.class, args);
        System.out.println("\n" +
                "╔════════════════════════════════════════╗\n" +
                "║  ✅ WebSocket Server Started!         ║\n" +
                "║  ws://localhost:8080/ws               ║\n" +
                "║  Ready to relay calls!                ║\n" +
                "╚════════════════════════════════════════╝\n");
    }
}

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

@Controller
class CallController {

    /**
     * ✅ FIXED: Receives call signal from caller and broadcasts to all subscribers
     * NOTE: @MessageMapping("call.start") NOT @MessageMapping("/app/call.start")
     * The /app prefix is already handled by WebSocketConfig.setApplicationDestinationPrefixes("/app")
     */
    @MessageMapping("call.start")
    @SendTo("/topic/calls")
    public String handleCallStart(String payload) {
        System.out.println("\n" +
                "╔════════════════════════════════════════╗\n" +
                "║  📡 SERVER RECEIVED CALL SIGNAL         ║\n" +
                "║     Client sent to: /app/call.start    ║\n" +
                "║     Payload Length: " + String.format("%-20s", payload.length() + " bytes") + "║\n" +
                "╚════════════════════════════════════════╝");

        System.out.println("📄 Full Payload: " + payload);

        try {
            System.out.println("\n🔊 BROADCASTING to /topic/calls...");
            System.out.println("   All subscribed users will receive this call signal\n");
            return payload;  // ✅ Broadcast to all subscribers
        } catch (Exception e) {
            System.err.println("❌ Error in handleCallStart: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ✅ FIXED: Receives call acceptance and broadcasts
     */
    @MessageMapping("call.accept")
    @SendTo("/topic/calls")
    public String handleCallAccept(String payload) {
        System.out.println("\n" +
                "╔════════════════════════════════════════╗\n" +
                "║  ✅ CALL ACCEPTED - BROADCASTING       ║\n" +
                "║     Destination: /topic/calls          ║\n" +
                "╚════════════════════════════════════════╝");
        System.out.println("📄 Payload: " + payload + "\n");
        return payload;
    }

    /**
     * ✅ FIXED: Receives call rejection and broadcasts
     */
    @MessageMapping("call.reject")
    @SendTo("/topic/calls")
    public String handleCallReject(String payload) {
        System.out.println("\n" +
                "╔════════════════════════════════════════╗\n" +
                "║  ❌ CALL REJECTED - BROADCASTING       ║\n" +
                "║     Destination: /topic/calls          ║\n" +
                "╚════════════════════════════════════════╝");
        System.out.println("📄 Payload: " + payload + "\n");
        return payload;
    }
}