package eafit.gruopChat.messaging.service;

import java.util.List;

import eafit.gruopChat.messaging.dto.MessageRequestDTO;
import eafit.gruopChat.messaging.dto.MessageResponseDTO;

public interface MessageService {

    // Guardar y retornar el mensaje (se llama desde el WebSocket controller)
    MessageResponseDTO sendMessage(Long senderId, MessageRequestDTO request);

    // Historial de mensajes de un canal (últimos N, paginado)
    List<MessageResponseDTO> getChannelMessages(Long channelId, int page, int size);

    // Historial de mensajes del grupo general (sin canal)
    List<MessageResponseDTO> getGroupMessages(Long groupId, int page, int size);

    
    // Eliminar mensaje (soft delete — solo el autor puede hacerlo)
    void deleteMessage(Long messageId, Long requestingUserId);

    // Editar mensaje (solo el autor, solo mensajes no eliminados)
    void editMessage(Long messageId, Long requestingUserId, String newContent);
}