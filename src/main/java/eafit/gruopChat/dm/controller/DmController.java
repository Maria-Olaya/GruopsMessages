package eafit.gruopChat.dm.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import eafit.gruopChat.dm.dto.ConversationResponseDTO;
import eafit.gruopChat.dm.dto.DmMessageRequestDTO;
import eafit.gruopChat.dm.dto.DmMessageResponseDTO;
import eafit.gruopChat.dm.service.DmService;

@RestController
public class DmController {

    private final DmService             dmService;
    private final SimpMessagingTemplate broker;

    public DmController(DmService dmService, SimpMessagingTemplate broker) {
        this.dmService = dmService;
        this.broker    = broker;
    }

    @PostMapping("/api/dm/start")
    public ResponseEntity<ConversationResponseDTO> start(
            @AuthenticationPrincipal Long userId,
            @RequestParam String targetEmail) {

        ConversationResponseDTO conv = dmService.startConversation(userId, targetEmail);

        // Notificar al destinatario si es solicitud pendiente
        if (conv.status() == eafit.gruopChat.dm.model.DirectConversation.Status.PENDING) {
            broker.convertAndSend("/topic/dm.requests." + conv.otherUserId(), conv);
        }
        return ResponseEntity.ok(conv);
    }

    @GetMapping("/api/dm")
    public ResponseEntity<List<ConversationResponseDTO>> list(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(dmService.listConversations(userId));
    }

    @GetMapping("/api/dm/requests")
    public ResponseEntity<List<ConversationResponseDTO>> pendingRequests(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(dmService.listPendingRequests(userId));
    }

    @PostMapping("/api/dm/{conversationId}/accept")
    public ResponseEntity<ConversationResponseDTO> accept(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal Long userId) {

        ConversationResponseDTO conv = dmService.acceptRequest(conversationId, userId);
        // Notificar al solicitante que fue aceptado
        broker.convertAndSend("/topic/dm.requests." + conv.otherUserId(), conv);
        return ResponseEntity.ok(conv);
    }

    @DeleteMapping("/api/dm/{conversationId}/decline")
    public ResponseEntity<Void> decline(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal Long userId) {
        dmService.declineRequest(conversationId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/dm/{conversationId}/messages")
    public ResponseEntity<List<DmMessageResponseDTO>> messages(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(dmService.getMessages(conversationId, userId, page, size));
    }

    @DeleteMapping("/api/dm/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal Long userId) {
        DmMessageResponseDTO updated = dmService.deleteMessage(messageId, userId);
        broker.convertAndSend("/topic/dm." + updated.conversationId(), updated);
        return ResponseEntity.noContent().build();
    }

    @MessageMapping("/dm.send")
    public void sendDm(@Payload DmMessageRequestDTO request, Principal principal) {
        Long senderId = Long.valueOf(principal.getName());
        DmMessageResponseDTO saved = dmService.sendMessage(senderId, request);
        broker.convertAndSend("/topic/dm." + saved.conversationId(), saved);
    }
}