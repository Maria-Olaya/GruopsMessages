package eafit.gruopChat.messaging.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eafit.gruopChat.group.exception.GroupNotFoundException;
import eafit.gruopChat.group.exception.NotMemberException;
import eafit.gruopChat.group.model.Channel;
import eafit.gruopChat.group.model.Group;
import eafit.gruopChat.group.repository.ChannelRepository;
import eafit.gruopChat.group.repository.GroupMemberRepository;
import eafit.gruopChat.group.repository.GroupRepository;
import eafit.gruopChat.messaging.dto.MessageRequestDTO;
import eafit.gruopChat.messaging.dto.MessageResponseDTO;
import eafit.gruopChat.messaging.model.Message;
import eafit.gruopChat.messaging.repository.MessageRepository;
import eafit.gruopChat.messaging.service.MessageService;
import eafit.gruopChat.shared.enums.MessageType;
import eafit.gruopChat.user.exception.UserNotFoundException;
import eafit.gruopChat.user.model.User;
import eafit.gruopChat.user.repository.UserRepository;

@Service
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final ChannelRepository channelRepository;
    private final GroupMemberRepository memberRepository;

    public MessageServiceImpl(MessageRepository messageRepository,
                               UserRepository userRepository,
                               GroupRepository groupRepository,
                               ChannelRepository channelRepository,
                               GroupMemberRepository memberRepository) {
        this.messageRepository = messageRepository;
        this.userRepository    = userRepository;
        this.groupRepository   = groupRepository;
        this.channelRepository = channelRepository;
        this.memberRepository  = memberRepository;
    }

    @Override
    public MessageResponseDTO sendMessage(Long senderId, MessageRequestDTO request) {
        // 1. Validar que el sender existe
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException(senderId));

        // 2. Validar que el grupo existe
        Group group = groupRepository.findById(request.groupId())
                .orElseThrow(() -> new GroupNotFoundException(request.groupId()));

        // 3. Validar que el sender es miembro del grupo
        if (!memberRepository.existsByGroupGroupIdAndUserUserId(request.groupId(), senderId)) {
            throw new NotMemberException(senderId, request.groupId());
        }

        // 4. Resolver canal (opcional)
        Channel channel = null;
        if (request.channelId() != null) {
            channel = channelRepository.findById(request.channelId())
                    .orElseThrow(() -> new RuntimeException("Canal no encontrado: " + request.channelId()));
            // Verificar que el canal pertenece al grupo
            if (!channel.getGroup().getGroupId().equals(request.groupId())) {
                throw new IllegalArgumentException("El canal no pertenece al grupo");
            }
        }

        // 5. Validar contenido según tipo
        if (request.type() == MessageType.TEXT && (request.content() == null || request.content().isBlank())) {
            throw new IllegalArgumentException("El contenido no puede estar vacío");
        }
        if ((request.type() == MessageType.IMAGE || request.type() == MessageType.FILE)
                && (request.fileUrl() == null || request.fileUrl().isBlank())) {
            throw new IllegalArgumentException("La URL del archivo es requerida");
        }

        // 6. Crear y guardar
        Message message = new Message();
        message.setSender(sender);
        message.setGroup(group);
        message.setChannel(channel);
        message.setType(request.type());
        message.setContent(request.content());
        message.setFileUrl(request.fileUrl());
        message.setFileName(request.fileName());

        return toDTO(messageRepository.save(message));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponseDTO> getChannelMessages(Long channelId, int page, int size) {
        return messageRepository
                .findByChannelId(channelId, PageRequest.of(page, size))
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponseDTO> getGroupMessages(Long groupId, int page, int size) {
        return messageRepository
                .findByGroupId(groupId, PageRequest.of(page, size))
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public void deleteMessage(Long messageId, Long requestingUserId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Mensaje no encontrado: " + messageId));

        if (!message.getSender().getUserId().equals(requestingUserId)) {
            throw new IllegalArgumentException("Solo el autor puede eliminar el mensaje");
        }

        message.setDeleted(true);
    }

    @Override 
    public void editMessage(Long messageId, Long requestingUserId, String newContent) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Mensaje no encontrado: " + messageId));

        if (!message.getSender().getUserId().equals(requestingUserId)) {
            throw new IllegalArgumentException("Solo el autor puede editar el mensaje");
        }
        if (message.isDeleted()) {
            throw new IllegalArgumentException("No se puede editar un mensaje eliminado");
        }
        if (newContent == null || newContent.isBlank()) {
            throw new IllegalArgumentException("El contenido no puede estar vacío");
        }

        message.setContent(newContent);
        message.setEditedAt(LocalDateTime.now());
    }


    // ===== Mapper =====
    private MessageResponseDTO toDTO(Message m) {
        return new MessageResponseDTO(
                m.getMessageId(),
                m.getGroup().getGroupId(),
                m.getChannel() != null ? m.getChannel().getChannelId() : null,
                m.getSender().getUserId(),
                m.getSender().getName(),
                m.getType(),
                m.isDeleted() ? null : m.getContent(),
                m.isDeleted() ? null : m.getFileUrl(),
                m.isDeleted() ? null : m.getFileName(),
                m.getSentAt(),
                m.getEditedAt(),
                m.isDeleted()
        );
    }
}