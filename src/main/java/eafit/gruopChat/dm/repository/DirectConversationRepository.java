package eafit.gruopChat.dm.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import eafit.gruopChat.dm.model.DirectConversation;
import eafit.gruopChat.dm.model.DirectConversation.Status;

public interface DirectConversationRepository extends JpaRepository<DirectConversation, Long> {

    @Query("SELECT dc FROM DirectConversation dc WHERE " +
           "(dc.userA.userId = :uid1 AND dc.userB.userId = :uid2) OR " +
           "(dc.userA.userId = :uid2 AND dc.userB.userId = :uid1)")
    Optional<DirectConversation> findBetween(@Param("uid1") Long uid1, @Param("uid2") Long uid2);

    @Query("SELECT dc FROM DirectConversation dc WHERE " +
           "dc.userA.userId = :userId OR dc.userB.userId = :userId " +
           "ORDER BY dc.createdAt DESC")
    List<DirectConversation> findAllByUserId(@Param("userId") Long userId);

    // Solicitudes PENDING donde YO soy el destinatario (no quien inició)
    @Query("SELECT dc FROM DirectConversation dc WHERE " +
           "dc.status = :status AND dc.requestedBy.userId != :userId AND " +
           "(dc.userA.userId = :userId OR dc.userB.userId = :userId)")
    List<DirectConversation> findPendingIncoming(
            @Param("userId") Long userId,
            @Param("status") Status status);
}