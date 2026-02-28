package eafit.gruopChat.messaging.dto;

// El cliente manda esto a /app/chat.read cuando el usuario ve un mensaje
public record ReceiptEvent(
        Long messageId,
        Long groupId,
        Long channelId   // null si es mensaje del grupo general
) {}