package gr.ihu.escapehub.lobby.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Collection;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LobbyStateResponseDto(
        MessageType type,
        int count,
        int required,
        Integer maxPlayers,
        Collection<String> players,
        String reason
) {}