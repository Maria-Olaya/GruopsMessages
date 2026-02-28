package eafit.gruopChat.messaging.model;

import java.time.LocalDateTime;

import eafit.gruopChat.shared.enums.MessageType;
import eafit.gruopChat.user.model.User;
import eafit.gruopChat.group.model.Group;
import eafit.gruopChat.group.model.Channel;
import eafit.gruopChat.shared.enums.MessageStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    // Quién lo envió
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // Siempre va a un grupo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    // Opcionalmente a un canal dentro del grupo (null = mensaje del grupo general)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id")
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type = MessageType.TEXT;

    // Contenido de texto (null si es imagen/archivo)
    @Column(columnDefinition = "TEXT")
    private String content;

    // URL del archivo/imagen (null si es texto)
    @Column(name = "file_url")
    private String fileUrl;

    // Nombre original del archivo (para mostrar al usuario)
    @Column(name = "file_name")
    private String fileName;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Column(nullable = false)
    private boolean deleted = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status = MessageStatus.SENT;

    // ===== Getters y Setters =====

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }

    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public LocalDateTime getEditedAt() { return editedAt; }
    public void setEditedAt(LocalDateTime editedAt) { this.editedAt = editedAt; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }   
}