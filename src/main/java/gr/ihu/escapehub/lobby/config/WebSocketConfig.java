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

    private final LobbyHandler lobbyHandler;
    private final MovementHandler movementHandler;
    private final String lobbyEndpoint;
    private final String[] allowedOrigins;

    public WebSocketConfig(
            LobbyHandler lobbyHandler,
            MovementHandler movementHandler,
            @Value("${escapehub.websocket.lobby-endpoint:/ws/lobby}")
            String lobbyEndpoint,
            @Value("${escapehub.websocket.allowed-origins:}")
            String[] allowedOrigins
    ) {
        this.lobbyHandler = lobbyHandler;
        this.movementHandler = movementHandler;
        this.lobbyEndpoint = lobbyEndpoint;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(
            WebSocketHandlerRegistry registry
    ) {
        registry.addHandler(lobbyHandler, lobbyEndpoint)
                .setAllowedOrigins(allowedOrigins);

        registry.addHandler(movementHandler, "/ws/game");
    }
}