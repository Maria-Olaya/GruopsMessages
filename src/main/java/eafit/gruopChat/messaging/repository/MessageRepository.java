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
    @Modifying
    @Query("DELETE FROM Message m WHERE m.channel.channelId = :channelId")
    void deleteByChannelId(@Param("channelId") Long channelId);

    // Borrar TODOS los mensajes de un grupo
    @Modifying
    @Query("DELETE FROM Message m WHERE m.group.groupId = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);

    // Para marcar como DELIVERED al conectarse — solo los que siguen en SENT
    @Query("SELECT m FROM Message m WHERE m.group.groupId = :groupId AND m.status = :status")
    List<Message> findByGroupGroupIdAndStatus(
        @Param("groupId") Long groupId,
        @Param("status") eafit.gruopChat.shared.enums.MessageStatus status
    );

    // Archivos (IMAGE + FILE) de un canal específico, ordenados más reciente primero
    @Query("SELECT m FROM Message m WHERE m.channel.channelId = :channelId " +
           "AND m.type IN (eafit.gruopChat.shared.enums.MessageType.IMAGE, " +
           "              eafit.gruopChat.shared.enums.MessageType.FILE) " +
           "AND m.deleted = false " +
           "ORDER BY m.sentAt DESC")
    List<Message> findFilesByChannelId(@Param("channelId") Long channelId);


    @Query("SELECT m FROM Message m WHERE m.channel IS NULL AND m.group.groupId = :groupId " +
       "AND m.type IN (eafit.gruopChat.shared.enums.MessageType.IMAGE, " +
       "              eafit.gruopChat.shared.enums.MessageType.FILE) " +
       "AND m.deleted = false ORDER BY m.sentAt DESC")
    List<Message> findFilesByGroupId(@Param("groupId") Long groupId);
}