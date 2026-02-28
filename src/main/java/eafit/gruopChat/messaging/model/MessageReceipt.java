package eafit.gruopChat.messaging.model;

import java.time.LocalDateTime;

import eafit.gruopChat.user.model.User;
import jakarta.persistence.*;

@Entity
@Table(
    name = "message_receipts",
    uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "user_id"})
)
public class MessageReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long receiptId;

    // El mensaje que fue leído
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    // El miembro que lo leyó
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Cuándo lo leyó
    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt = LocalDateTime.now();

    // ===== Getters & Setters =====

    public Long getReceiptId() { return receiptId; }

    public Message getMessage() { return message; }
    public void setMessage(Message message) { this.message = message; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}