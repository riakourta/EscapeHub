package gr.ihu.escapehub.lobby.dto;

public record LobbyMessageDto(
        MessageType type,
        String username,
        String token
) {}