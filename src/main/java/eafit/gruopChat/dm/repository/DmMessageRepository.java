package eafit.gruopChat.dm.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import eafit.gruopChat.dm.model.DmMessage;

public interface DmMessageRepository extends JpaRepository<DmMessage, Long> {

    @Query("SELECT m FROM DmMessage m WHERE m.conversation.conversationId = :convId " +
           "ORDER BY m.sentAt ASC")
    List<DmMessage> findByConversationId(@Param("convId") Long convId, Pageable pageable);
}