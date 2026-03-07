package eafit.gruopChat.dm.model;

import java.time.LocalDateTime;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "direct_conversations",
    uniqueConstraints = { @UniqueConstraint(columnNames = {"user_a_id", "user_b_id"}) })
public class DirectConversation {

    public enum Status { PENDING, ACTIVE }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conversation_id")
    private Long conversationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_a_id", nullable = false)
    private User userA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_b_id", nullable = false)
    private User userB;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    public Long getConversationId()              { return conversationId; }
    public User getUserA()                       { return userA; }
    public void setUserA(User u)                 { this.userA = u; }
    public User getUserB()                       { return userB; }
    public void setUserB(User u)                 { this.userB = u; }
    public User getRequestedBy()                 { return requestedBy; }
    public void setRequestedBy(User u)           { this.requestedBy = u; }
    public Status getStatus()                    { return status; }
    public void setStatus(Status s)              { this.status = s; }
    public LocalDateTime getCreatedAt()          { return createdAt; }
    public LocalDateTime getRespondedAt()        { return respondedAt; }
    public void setRespondedAt(LocalDateTime t)  { this.respondedAt = t; }
}