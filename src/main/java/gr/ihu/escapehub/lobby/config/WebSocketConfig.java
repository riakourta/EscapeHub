package gr.ihu.escapehub.lobby.config;

import gr.ihu.escapehub.lobby.handler.LobbyHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LobbyHandler lobbyHandler;
    private final String lobbyEndpoint;
    private final String gameEndpoint;
    private final String[] allowedOrigins;

    public WebSocketConfig(
            LobbyHandler lobbyHandler,
            @Value("${escapehub.websocket.lobby-endpoint}") String lobbyEndpoint,
            @Value("${escapehub.websocket.game-endpoint:/ws/game}") String gameEndpoint,
            @Value("${escapehub.websocket.allowed-origins}") String[] allowedOrigins) {
        this.lobbyHandler = lobbyHandler;
        this.lobbyEndpoint = lobbyEndpoint;
        this.gameEndpoint = gameEndpoint;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(lobbyHandler, lobbyEndpoint)
                .setAllowedOrigins(allowedOrigins);

        if (gameEndpoint != null && !gameEndpoint.isBlank() && !gameEndpoint.equals(lobbyEndpoint)) {
            registry.addHandler(lobbyHandler, gameEndpoint)
                    .setAllowedOrigins(allowedOrigins);
        }
    }
}