package eafit.gruopChat.dm.service;

import java.util.List;

import eafit.gruopChat.dm.dto.ConversationResponseDTO;
import eafit.gruopChat.dm.dto.DmMessageRequestDTO;
import eafit.gruopChat.dm.dto.DmMessageResponseDTO;

public interface DmService {
    ConversationResponseDTO  startConversation(Long requestingUserId, String targetEmail);
    List<ConversationResponseDTO> listConversations(Long userId);
    List<ConversationResponseDTO> listPendingRequests(Long userId);
    ConversationResponseDTO  acceptRequest(Long conversationId, Long userId);
    void                     declineRequest(Long conversationId, Long userId);
    List<DmMessageResponseDTO> getMessages(Long conversationId, Long requestingUserId, int page, int size);
    DmMessageResponseDTO     sendMessage(Long senderId, DmMessageRequestDTO request);
    DmMessageResponseDTO     deleteMessage(Long messageId, Long requestingUserId);
}