package eafit.gruopChat.group.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import eafit.gruopChat.group.model.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query("SELECT g FROM Group g JOIN g.members m WHERE m.user.userId = :userId")
    List<Group> findGroupsByMemberUserId(@Param("userId") Long userId);

    Optional<Group> findByInviteCode(String inviteCode);
}