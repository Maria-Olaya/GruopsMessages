package eafit.gruopChat.dm.dto;

import java.time.LocalDateTime;

import eafit.gruopChat.shared.enums.MessageType;

public record DmMessageResponseDTO(
    Long          messageId,
    Long          conversationId,
    Long          senderId,
    String        senderName,
    MessageType   type,
    String        content,
    String        fileUrl,
    String        fileName,
    LocalDateTime sentAt,
    boolean       deleted
) {}