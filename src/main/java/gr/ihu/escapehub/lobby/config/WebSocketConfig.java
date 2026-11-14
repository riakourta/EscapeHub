package gr.ihu.escapehub.lobby.config;

import gr.ihu.escapehub.lobby.handler.LobbyHandler;
import gr.ihu.escapehub.movement.handler.MovementHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final LobbyHandler lobby;
    private final MovementHandler movement;
    private final String lobbyEndpoint;
    public WebSocketConfig(LobbyHandler lobby, MovementHandler movement,
            @Value("${escapehub.websocket.lobby-endpoint:/ws/lobby}") String lobbyEndpoint) {
        this.lobby = lobby;
        this.movement = movement;
        this.lobbyEndpoint = lobbyEndpoint;
    }
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Spring's default same-origin check applies to both cookie-authenticated sockets.
        registry.addHandler(lobby, lobbyEndpoint);
        registry.addHandler(movement, "/ws/game");
    }
}
