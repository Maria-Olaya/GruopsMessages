package eafit.gruopChat.messaging.dto;

import eafit.gruopChat.shared.enums.MessageType;
import jakarta.validation.constraints.NotNull;

// DTO para ENVIAR un mensaje vía WebSocket
// El frontend manda esto al servidor
public record MessageRequestDTO(

        @NotNull
        Long groupId,

        // null si es mensaje del grupo general
        Long channelId,

        @NotNull
        MessageType type,

        // Requerido si type = TEXT
        String content,

        // Requerido si type = IMAGE o FILE
        String fileUrl,
        String fileName
) {}