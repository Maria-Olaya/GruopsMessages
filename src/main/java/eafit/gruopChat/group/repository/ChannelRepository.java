package eafit.gruopChat.group.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import eafit.gruopChat.group.model.Channel;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

    List<Channel> findByGroupGroupId(Long groupId);

    boolean existsByGroupGroupIdAndName(Long groupId, String name);
}