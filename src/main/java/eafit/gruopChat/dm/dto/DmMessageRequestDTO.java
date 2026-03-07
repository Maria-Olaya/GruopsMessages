package eafit.gruopChat.dm.dto;

import eafit.gruopChat.shared.enums.MessageType;

public record DmMessageRequestDTO(
    Long        conversationId,
    MessageType type,
    String      content,
    String      fileUrl,
    String      fileName
) {}