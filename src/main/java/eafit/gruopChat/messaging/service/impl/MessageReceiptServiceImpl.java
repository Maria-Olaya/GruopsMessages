package eafit.gruopChat.messaging.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eafit.gruopChat.group.repository.GroupMemberRepository;
import eafit.gruopChat.messaging.dto.ReceiptEvent;
import eafit.gruopChat.messaging.dto.ReceiptResponseDTO;
import eafit.gruopChat.messaging.model.Message;
import eafit.gruopChat.messaging.model.MessageReceipt;
import eafit.gruopChat.messaging.repository.MessageReceiptRepository;
import eafit.gruopChat.messaging.repository.MessageRepository;
import eafit.gruopChat.messaging.service.MessageReceiptService;
import eafit.gruopChat.shared.enums.MessageStatus;
import eafit.gruopChat.user.exception.UserNotFoundException;
import eafit.gruopChat.user.model.User;
import eafit.gruopChat.user.repository.UserRepository;

import java.util.List;

@Service
@Transactional
public class MessageReceiptServiceImpl implements MessageReceiptService {

    private final MessageReceiptRepository receiptRepository;
    private final MessageRepository        messageRepository;
    private final UserRepository           userRepository;
    private final GroupMemberRepository    memberRepository;

    public MessageReceiptServiceImpl(
            MessageReceiptRepository receiptRepository,
            MessageRepository messageRepository,
            UserRepository userRepository,
            GroupMemberRepository memberRepository) {
        this.receiptRepository = receiptRepository;
        this.messageRepository = messageRepository;
        this.userRepository    = userRepository;
        this.memberRepository  = memberRepository;
    }

    @Override
    public ReceiptResponseDTO markAsRead(Long userId, ReceiptEvent event) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Message message = messageRepository.findById(event.messageId())
                .orElseThrow(() -> new RuntimeException("Mensaje no encontrado: " + event.messageId()));

        // Si el sender se ve a sí mismo, ignorar — no cuenta como receipt
        if (message.getSender().getUserId().equals(userId)) {
            return buildResponse(event, userId, user.getName(), message.getStatus());
        }

        // Si ya existe el receipt de este usuario, no duplicar
        boolean yaLeyo = receiptRepository
                .findByMessageMessageIdAndUserUserId(event.messageId(), userId)
                .isPresent();

        if (!yaLeyo) {
            MessageReceipt receipt = new MessageReceipt();
            receipt.setMessage(message);
            receipt.setUser(user);
            receiptRepository.save(receipt);
        }

        // Calcular el nuevo status
        MessageStatus nuevoStatus = calcularStatus(message, event.groupId(), event.channelId());
        message.setStatus(nuevoStatus);

        return buildResponse(event, userId, user.getName(), nuevoStatus);
    }

    @Override
    public void markPendingAsDelivered(Long userId, Long groupId) {
        // Traer todos los mensajes del grupo que aún están en SENT
        List<Message> pendientes = messageRepository
                .findByGroupGroupIdAndStatus(groupId, MessageStatus.SENT);

        for (Message msg : pendientes) {
            // No marcar los propios mensajes del usuario
            if (!msg.getSender().getUserId().equals(userId)) {
                msg.setStatus(MessageStatus.DELIVERED);
            }
        }
        // El @Transactional guarda los cambios automáticamente (dirty checking)
    }

    // ===== Lógica central =====

    private MessageStatus calcularStatus(Message message, Long groupId, Long channelId) {
        // Total de miembros del grupo (excluyendo al sender)
        long totalMiembros = memberRepository
                .countByGroupGroupId(groupId) - 1;

        if (totalMiembros <= 0) return MessageStatus.READ;

        // Cuántos han leído (receipts guardados)
        long totalQueHanLeido = receiptRepository
                .countByMessageMessageId(message.getMessageId());

        // Si todos leyeron → READ, si no → DELIVERED
        return (totalQueHanLeido >= totalMiembros)
                ? MessageStatus.READ
                : MessageStatus.DELIVERED;
    }

    private ReceiptResponseDTO buildResponse(
            ReceiptEvent event, Long userId, String userName, MessageStatus status) {
        return new ReceiptResponseDTO(
                event.messageId(),
                event.groupId(),
                event.channelId(),
                userId,
                userName,
                status
        );
    }
}