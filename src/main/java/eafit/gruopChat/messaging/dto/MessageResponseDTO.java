package eafit.gruopChat.messaging.dto;

import java.time.LocalDateTime;
import eafit.gruopChat.shared.enums.MessageType;

// DTO que el servidor envía al frontend (via WebSocket y via REST)
public record MessageResponseDTO(
        Long messageId,
        Long groupId,
        Long channelId,        // null si es mensaje general del grupo
        Long senderId,
        String senderName,
        MessageType type,
        String content,        // null si es imagen/archivo
        String fileUrl,        // null si es texto
        String fileName,       // null si es texto
        LocalDateTime sentAt,
        LocalDateTime editedAt,
        boolean deleted
) {}