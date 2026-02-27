package eafit.gruopChat.presence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import eafit.gruopChat.presence.model.MessageRead;

public interface MessageReadRepository extends JpaRepository<MessageRead, Long> {

    @Query("SELECT COUNT(mr) FROM MessageRead mr WHERE mr.message.messageId = :messageId")
    long countReadersByMessageId(@Param("messageId") Long messageId);

    @Query("SELECT m.messageId FROM Message m WHERE m.channel.channelId = :channelId " +
           "AND m.deleted = false " +
           "AND m.sender.userId != :userId " +
           "AND NOT EXISTS (SELECT mr FROM MessageRead mr WHERE mr.message = m AND mr.user.userId = :userId)")
    List<Long> findUnreadMessageIdsByChannelAndUser(
            @Param("channelId") Long channelId, @Param("userId") Long userId);

    @Query("SELECT m.messageId FROM Message m WHERE m.group.groupId = :groupId " +
           "AND m.channel IS NULL " +
           "AND m.deleted = false " +
           "AND m.sender.userId != :userId " +
           "AND NOT EXISTS (SELECT mr FROM MessageRead mr WHERE mr.message = m AND mr.user.userId = :userId)")
    List<Long> findUnreadMessageIdsByGroupGeneralAndUser(
            @Param("groupId") Long groupId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM MessageRead mr WHERE mr.message.channel.channelId = :channelId")
    void deleteByChannelId(@Param("channelId") Long channelId);

    @Modifying
    @Query("DELETE FROM MessageRead mr WHERE mr.message.group.groupId = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);
}