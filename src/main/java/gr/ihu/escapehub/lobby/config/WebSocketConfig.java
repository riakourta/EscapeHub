package gr.ihu.escapehub.lobby.config;

import gr.ihu.escapehub.movement.handler.MovementHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final MovementHandler movement;
    public WebSocketConfig(MovementHandler movement) { this.movement = movement; }
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(movement, "/ws/game");
    }
}
