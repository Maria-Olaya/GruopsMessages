package eafit.gruopChat.messaging.dto;

import eafit.gruopChat.shared.enums.MessageStatus;

// El servidor broadcast esto a /topic/receipts.{channelId} o /topic/receipts.group.{groupId}
public record ReceiptResponseDTO(
        Long messageId,
        Long groupId,
        Long channelId,       // null si es mensaje del grupo general
        Long readByUserId,    // quién acaba de leer
        String readByName,
        MessageStatus status  // DELIVERED o READ según corresponda
) {}