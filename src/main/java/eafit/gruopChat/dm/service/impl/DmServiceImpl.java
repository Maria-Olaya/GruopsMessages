package eafit.gruopChat.dm.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eafit.gruopChat.dm.dto.ConversationResponseDTO;
import eafit.gruopChat.dm.dto.DmMessageRequestDTO;
import eafit.gruopChat.dm.dto.DmMessageResponseDTO;
import eafit.gruopChat.dm.model.DirectConversation;
import eafit.gruopChat.dm.model.DirectConversation.Status;
import eafit.gruopChat.dm.model.DmMessage;
import eafit.gruopChat.dm.repository.DirectConversationRepository;
import eafit.gruopChat.dm.repository.DmMessageRepository;
import eafit.gruopChat.dm.service.DmService;
import eafit.gruopChat.group.repository.GroupMemberRepository;
import eafit.gruopChat.shared.enums.MessageType;
import eafit.gruopChat.user.exception.UserNotFoundException;
import eafit.gruopChat.user.model.User;
import eafit.gruopChat.user.repository.UserRepository;

@Service
@Transactional
public class DmServiceImpl implements DmService {

    private final DirectConversationRepository convRepo;
    private final DmMessageRepository          msgRepo;
    private final UserRepository               userRepo;
    private final GroupMemberRepository        memberRepo;

    public DmServiceImpl(DirectConversationRepository convRepo,
                         DmMessageRepository msgRepo,
                         UserRepository userRepo,
                         GroupMemberRepository memberRepo) {
        this.convRepo   = convRepo;
        this.msgRepo    = msgRepo;
        this.userRepo   = userRepo;
        this.memberRepo = memberRepo;
    }

    @Override
    public ConversationResponseDTO startConversation(Long requestingUserId, String targetEmail) {
        User me     = findUser(requestingUserId);
        User target = userRepo.findByEmail(targetEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + targetEmail));

        if (me.getUserId().equals(target.getUserId()))
            throw new IllegalArgumentException("No puedes iniciar una conversación contigo mismo");

        // Si ya existe, retornar tal cual
        return convRepo.findBetween(me.getUserId(), target.getUserId())
                .map(dc -> toDTO(dc, requestingUserId))
                .orElseGet(() -> createNew(me, target));
    }

    private ConversationResponseDTO createNew(User me, User target) {
        boolean shareGroup = shareAnyGroup(me.getUserId(), target.getUserId());

        DirectConversation dc = new DirectConversation();
        // Garantizar userA.id < userB.id para la unicidad
        if (me.getUserId() < target.getUserId()) { dc.setUserA(me); dc.setUserB(target); }
        else                                      { dc.setUserA(target); dc.setUserB(me); }

        dc.setRequestedBy(me);
        dc.setStatus(shareGroup ? Status.ACTIVE : Status.PENDING);

        return toDTO(convRepo.save(dc), me.getUserId());
    }

    private boolean shareAnyGroup(Long uid1, Long uid2) {
        Set<Long> groupsA = memberRepo.findByUserUserId(uid1).stream()
                .map(m -> m.getGroup().getGroupId()).collect(Collectors.toSet());
        return memberRepo.findByUserUserId(uid2).stream()
                .anyMatch(m -> groupsA.contains(m.getGroup().getGroupId()));
    }

    @Override @Transactional(readOnly = true)
    public List<ConversationResponseDTO> listConversations(Long userId) {
        return convRepo.findAllByUserId(userId).stream()
                .map(dc -> toDTO(dc, userId)).collect(Collectors.toList());
    }

    @Override @Transactional(readOnly = true)
    public List<ConversationResponseDTO> listPendingRequests(Long userId) {
        return convRepo.findPendingIncoming(userId, Status.PENDING).stream()
                .map(dc -> toDTO(dc, userId)).collect(Collectors.toList());
    }

    @Override
    public ConversationResponseDTO acceptRequest(Long conversationId, Long userId) {
        DirectConversation dc = findConv(conversationId);
        assertIsRecipient(dc, userId);
        assertStatus(dc, Status.PENDING);
        dc.setStatus(Status.ACTIVE);
        dc.setRespondedAt(LocalDateTime.now());
        return toDTO(dc, userId);
    }

    @Override
    public void declineRequest(Long conversationId, Long userId) {
        DirectConversation dc = findConv(conversationId);
        assertIsRecipient(dc, userId);
        assertStatus(dc, Status.PENDING);
        convRepo.delete(dc);
    }

    @Override @Transactional(readOnly = true)
    public List<DmMessageResponseDTO> getMessages(Long conversationId, Long requestingUserId, int page, int size) {
        DirectConversation dc = findConv(conversationId);
        assertParticipant(dc, requestingUserId);
        if (dc.getStatus() == Status.PENDING && !dc.getRequestedBy().getUserId().equals(requestingUserId))
            throw new IllegalArgumentException("Acepta la solicitud para ver los mensajes");
        return msgRepo.findByConversationId(conversationId, PageRequest.of(page, size))
                .stream().map(this::toMsgDTO).collect(Collectors.toList());
    }

    @Override
    public DmMessageResponseDTO sendMessage(Long senderId, DmMessageRequestDTO req) {
        DirectConversation dc = findConv(req.conversationId());
        assertParticipant(dc, senderId);
        if (dc.getStatus() == Status.PENDING && !dc.getRequestedBy().getUserId().equals(senderId))
            throw new IllegalArgumentException("Acepta la solicitud para poder responder");

        User sender = findUser(senderId);
        if (req.type() == MessageType.TEXT && (req.content() == null || req.content().isBlank()))
            throw new IllegalArgumentException("El contenido no puede estar vacío");

        DmMessage msg = new DmMessage();
        msg.setConversation(dc);
        msg.setSender(sender);
        msg.setType(req.type() != null ? req.type() : MessageType.TEXT);
        msg.setContent(req.content());
        msg.setFileUrl(req.fileUrl());
        msg.setFileName(req.fileName());
        return toMsgDTO(msgRepo.save(msg));
    }

    @Override
    public DmMessageResponseDTO deleteMessage(Long messageId, Long requestingUserId) {
        DmMessage msg = msgRepo.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Mensaje no encontrado: " + messageId));
        if (!msg.getSender().getUserId().equals(requestingUserId))
            throw new IllegalArgumentException("Solo el autor puede eliminar el mensaje");
        msg.setDeleted(true);
        return toMsgDTO(msg);
    }

    // ── helpers ──────────────────────────────────────────────
    private User findUser(Long id) {
        return userRepo.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
    private DirectConversation findConv(Long id) {
        return convRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Conversación no encontrada: " + id));
    }
    private void assertParticipant(DirectConversation dc, Long userId) {
        if (!dc.getUserA().getUserId().equals(userId) && !dc.getUserB().getUserId().equals(userId))
            throw new IllegalArgumentException("No eres participante de esta conversación");
    }
    private void assertIsRecipient(DirectConversation dc, Long userId) {
        if (dc.getRequestedBy().getUserId().equals(userId))
            throw new IllegalArgumentException("No puedes aceptar tu propia solicitud");
        assertParticipant(dc, userId);
    }
    private void assertStatus(DirectConversation dc, Status expected) {
        if (dc.getStatus() != expected)
            throw new IllegalArgumentException("Estado inválido: " + dc.getStatus());
    }

    // ── mappers ───────────────────────────────────────────────
    private ConversationResponseDTO toDTO(DirectConversation dc, Long requestingUserId) {
        User other = dc.getUserA().getUserId().equals(requestingUserId) ? dc.getUserB() : dc.getUserA();
        boolean isIncoming = dc.getStatus() == Status.PENDING
                && !dc.getRequestedBy().getUserId().equals(requestingUserId);
        return new ConversationResponseDTO(
                dc.getConversationId(), other.getUserId(), other.getName(), other.getEmail(),
                dc.getStatus(), isIncoming, dc.getCreatedAt());
    }

    private DmMessageResponseDTO toMsgDTO(DmMessage m) {
        return new DmMessageResponseDTO(
                m.getMessageId(), m.getConversation().getConversationId(),
                m.getSender().getUserId(), m.getSender().getName(),
                m.getType(),
                m.isDeleted() ? null : m.getContent(),
                m.isDeleted() ? null : m.getFileUrl(),
                m.isDeleted() ? null : m.getFileName(),
                m.getSentAt(), m.isDeleted());
    }
}