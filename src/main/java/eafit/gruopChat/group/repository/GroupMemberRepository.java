package eafit.gruopChat.group.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import eafit.gruopChat.group.model.GroupMember;
import eafit.gruopChat.shared.enums.GroupRole;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    Optional<GroupMember> findByGroupGroupIdAndUserUserId(Long groupId, Long userId);

    boolean existsByGroupGroupIdAndUserUserId(Long groupId, Long userId);

    List<GroupMember> findByGroupGroupId(Long groupId);

    List<GroupMember> findByGroupGroupIdAndRole(Long groupId, GroupRole role);

    void deleteByGroupGroupIdAndUserUserId(Long groupId, Long userId);
    // Necesario para Presence: todos los grupos donde está un usuario
    List<GroupMember> findByUserUserId(Long userId);
}