package eafit.gruopChat.messaging.service;

import eafit.gruopChat.messaging.dto.ReceiptEvent;
import eafit.gruopChat.messaging.dto.ReceiptResponseDTO;

public interface MessageReceiptService {

    // Llamado cuando un usuario lee un mensaje
    // Retorna el DTO con el status actualizado para hacer broadcast
    ReceiptResponseDTO markAsRead(Long userId, ReceiptEvent event);

    // Llamado cuando un usuario se conecta por WebSocket
    // Marca como DELIVERED todos los mensajes que aún no llegaron
    void markPendingAsDelivered(Long userId, Long groupId);
}