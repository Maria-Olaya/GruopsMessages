package eafit.gruopChat.messaging.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import eafit.gruopChat.messaging.model.MessageReceipt;

public interface MessageReceiptRepository extends JpaRepository<MessageReceipt, Long> {

    // ¿Ya leyó este usuario este mensaje?
    Optional<MessageReceipt> findByMessageMessageIdAndUserUserId(Long messageId, Long userId);

    // Cuántos usuarios han leído este mensaje
    long countByMessageMessageId(Long messageId);

    // Todos los receipts de un mensaje (para saber quiénes leyeron)
    List<MessageReceipt> findByMessageMessageId(Long messageId);

    // IDs de mensajes leídos por un usuario en un canal
    @Query("""
        SELECT r.message.messageId
        FROM MessageReceipt r
        WHERE r.user.userId = :userId
          AND r.message.channel.channelId = :channelId
    """)
    List<Long> findReadMessageIdsByUserAndChannel(
        @Param("userId") Long userId,
        @Param("channelId") Long channelId
    );
}