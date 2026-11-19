package gr.ihu.escapehub.lobby.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessageDto(
        MessageType type,
        String sender,
        String color,
        String message,
        long timestamp
) {}