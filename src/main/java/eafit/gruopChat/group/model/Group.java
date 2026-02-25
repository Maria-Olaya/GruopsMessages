package eafit.gruopChat.group.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import eafit.gruopChat.user.model.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "groups")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long groupId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    // El creador original del grupo — no cambia aunque haya más admins
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Relaciones — solo para navegación interna, no se exponen directamente
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Channel> channels = new ArrayList<>();

    // Cascade a invitations para que al borrar el grupo se borren sus invitaciones
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupInvitation> invitations = new ArrayList<>();


    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ======= Getters & Setters =======

    public Long getGroupId() { return groupId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean isPrivate) { this.isPrivate = isPrivate; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public List<GroupMember> getMembers() { return members; }
    public List<Channel> getChannels() { return channels; }
    public List<GroupInvitation> getInvitations() { return invitations; }
    
}