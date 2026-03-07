package eafit.gruopChat.messaging.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import eafit.gruopChat.messaging.dto.MessageRequestDTO;
import eafit.gruopChat.messaging.dto.MessageResponseDTO;
import eafit.gruopChat.messaging.dto.ReceiptEvent;
import eafit.gruopChat.messaging.dto.ReceiptResponseDTO;
import eafit.gruopChat.messaging.service.MessageReceiptService;
import eafit.gruopChat.messaging.service.MessageService;


@RestController
public class MessageController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageReceiptService receiptService;

    public MessageController(MessageService messageService,
                            SimpMessagingTemplate messagingTemplate,
                            MessageReceiptService receiptService) {
        this.messageService    = messageService;
        this.messagingTemplate = messagingTemplate;
        this.receiptService    = receiptService;
    }

    // ===================== WEBSOCKET =====================

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload MessageRequestDTO request, Principal principal) {
        Long senderId = Long.valueOf(principal.getName());

        MessageResponseDTO saved = messageService.sendMessage(senderId, request);

        if (saved.channelId() != null) {
            messagingTemplate.convertAndSend(
                "/topic/channel." + saved.channelId(), saved);
        } else {
            messagingTemplate.convertAndSend(
                "/topic/group." + saved.groupId(), saved);
        }
    }

    @MessageMapping("/chat.read")
    public void markAsRead(@Payload ReceiptEvent event, Principal principal) {
        Long userId = Long.valueOf(principal.getName());

        ReceiptResponseDTO receipt = receiptService.markAsRead(userId, event);

        if (receipt.channelId() != null) {
            messagingTemplate.convertAndSend(
                "/topic/receipts.channel." + receipt.channelId(), receipt);
        } else {
            messagingTemplate.convertAndSend(
                "/topic/receipts.group." + receipt.groupId(), receipt);
        }
    }

    // ===================== REST — HISTORIAL =====================

    // GET /api/messages/channel/{channelId}?page=0&size=50
    @GetMapping("/api/messages/channel/{channelId}")
    public ResponseEntity<List<MessageResponseDTO>> getChannelMessages(
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(messageService.getChannelMessages(channelId, page, size));
    }

    // GET /api/messages/group/{groupId}?page=0&size=50
    @GetMapping("/api/messages/group/{groupId}")
    public ResponseEntity<List<MessageResponseDTO>> getGroupMessages(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(messageService.getGroupMessages(groupId, page, size));
    }

    // DELETE /api/messages/{messageId}
    @DeleteMapping("/api/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal Long userId) {
        messageService.deleteMessage(messageId, userId);
        return ResponseEntity.noContent().build();
    }

    // PATCH /api/messages/{messageId}?content=...
    @PatchMapping("/api/messages/{messageId}")
    public ResponseEntity<Void> editMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal Long userId,
            @RequestParam String content) {
        messageService.editMessage(messageId, userId, content);
        return ResponseEntity.noContent().build();
    }

    // GET /api/messages/channel/{channelId}/files
    // Devuelve archivos (IMAGE + FILE) del canal, ordenados más reciente primero
    @GetMapping("/api/messages/channel/{channelId}/files")
    public ResponseEntity<List<MessageResponseDTO>> getChannelFiles(
            @PathVariable Long channelId) {
        return ResponseEntity.ok(messageService.getChannelFiles(channelId));
    }


    @GetMapping("/api/messages/group/{groupId}/files")
    public ResponseEntity<List<MessageResponseDTO>> getGroupFiles(
            @PathVariable Long groupId) {
        return ResponseEntity.ok(messageService.getGroupFiles(groupId));
    }
}