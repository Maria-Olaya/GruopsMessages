package eafit.gruopChat.dm.model;

import java.time.LocalDateTime;

import eafit.gruopChat.shared.enums.MessageType;
import eafit.gruopChat.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "dm_messages")
public class DmMessage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private DirectConversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type = MessageType.TEXT;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "file_url")  private String fileUrl;
    @Column(name = "file_name") private String fileName;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() { this.sentAt = LocalDateTime.now(); }

    public Long getMessageId()                        { return messageId; }
    public DirectConversation getConversation()       { return conversation; }
    public void setConversation(DirectConversation c) { this.conversation = c; }
    public User getSender()                           { return sender; }
    public void setSender(User s)                     { this.sender = s; }
    public MessageType getType()                      { return type; }
    public void setType(MessageType t)                { this.type = t; }
    public String getContent()                        { return content; }
    public void setContent(String c)                  { this.content = c; }
    public String getFileUrl()                        { return fileUrl; }
    public void setFileUrl(String u)                  { this.fileUrl = u; }
    public String getFileName()                       { return fileName; }
    public void setFileName(String n)                 { this.fileName = n; }
    public boolean isDeleted()                        { return deleted; }
    public void setDeleted(boolean d)                 { this.deleted = d; }
    public LocalDateTime getSentAt()                  { return sentAt; }
}