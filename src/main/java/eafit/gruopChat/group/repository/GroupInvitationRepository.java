package eafit.gruopChat.group.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import eafit.gruopChat.group.model.GroupInvitation;
import eafit.gruopChat.shared.enums.InvitationStatus;

public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, Long> {

    // Invitaciones pendientes para un usuario
    List<GroupInvitation> findByInvitedUserUserIdAndStatus(Long userId, InvitationStatus status);

    // Invitaciones enviadas para un grupo
    List<GroupInvitation> findByGroupGroupId(Long groupId);

    // Verificar si ya existe una invitación pendiente
    Optional<GroupInvitation> findByGroupGroupIdAndInvitedUserUserIdAndStatus(
        Long groupId, Long userId, InvitationStatus status);
}