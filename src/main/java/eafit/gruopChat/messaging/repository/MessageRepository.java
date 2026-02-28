package eafit.gruopChat.messaging.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import eafit.gruopChat.messaging.model.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // Incluye mensajes eliminados (deleted=true) para mostrar "Mensaje eliminado" a todos
    @Query("SELECT m FROM Message m WHERE m.channel.channelId = :channelId ORDER BY m.sentAt ASC")
    List<Message> findByChannelId(@Param("channelId") Long channelId, Pageable pageable);

    // Incluye mensajes eliminados
    @Query("SELECT m FROM Message m WHERE m.group.groupId = :groupId AND m.channel IS NULL ORDER BY m.sentAt ASC")
    List<Message> findByGroupId(@Param("groupId") Long groupId, Pageable pageable);

    // Para preview futuro
    @Query("SELECT m FROM Message m WHERE m.channel.channelId = :channelId ORDER BY m.sentAt DESC")
    List<Message> findLatestByChannelId(@Param("channelId") Long channelId, Pageable pageable);

    // Borrar físicamente los mensajes de un canal antes de borrar el canal
    // Sin esto, la FK messages.channel_id viola la constraint al hacer channelRepository.delete()
    @Modifying
    @Query("DELETE FROM Message m WHERE m.channel.channelId = :channelId")
    void deleteByChannelId(@Param("channelId") Long channelId);
    // Borrar TODOS los mensajes de un grupo (antes de borrar el grupo)
    // Cubre tanto mensajes de canales como del general (channel IS NULL)
    @Modifying
    @Query("DELETE FROM Message m WHERE m.group.groupId = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);

    // Para marcar como DELIVERED al conectarse — solo los que siguen en SENT
    @Query("SELECT m FROM Message m WHERE m.group.groupId = :groupId AND m.status = :status")
    List<Message> findByGroupGroupIdAndStatus(
        @Param("groupId") Long groupId,
        @Param("status") eafit.gruopChat.shared.enums.MessageStatus status
    );
}