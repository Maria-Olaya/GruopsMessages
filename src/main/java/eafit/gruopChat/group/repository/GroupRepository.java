package eafit.gruopChat.group.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import eafit.gruopChat.group.model.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {

    // Grupos creados por un usuario
    List<Group> findByCreatedByUserId(Long userId);

    // Grupos donde un usuario es miembro (via group_members)
    @Query("SELECT gm.group FROM GroupMember gm WHERE gm.user.userId = :userId")
    List<Group> findGroupsByMemberUserId(@Param("userId") Long userId);

    // Grupos públicos
    List<Group> findByIsPrivateFalse();
}