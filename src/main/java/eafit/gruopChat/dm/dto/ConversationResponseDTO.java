package eafit.gruopChat.dm.dto;

import java.time.LocalDateTime;

import eafit.gruopChat.dm.model.DirectConversation.Status;

public record ConversationResponseDTO(
    Long          conversationId,
    Long          otherUserId,
    String        otherUserName,
    String        otherUserEmail,
    Status        status,       // PENDING | ACTIVE
    boolean       isIncoming,   // true = yo soy el destinatario, debo aceptar/rechazar
    LocalDateTime createdAt
) {}