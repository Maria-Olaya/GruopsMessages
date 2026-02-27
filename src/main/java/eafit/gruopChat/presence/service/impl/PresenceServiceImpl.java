package eafit.gruopChat.presence.service.impl;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eafit.gruopChat.group.repository.GroupMemberRepository;
import eafit.gruopChat.presence.dto.PresenceEventDTO;
import eafit.gruopChat.presence.model.UserPresence;
import eafit.gruopChat.presence.repository.UserPresenceRepository;
import eafit.gruopChat.presence.service.PresenceService;
import eafit.gruopChat.user.model.User;
import eafit.gruopChat.user.repository.UserRepository;

@Service
@Transactional
public class PresenceServiceImpl implements PresenceService {

    private final Set<Long> onlineUsers = ConcurrentHashMap.newKeySet();

    private final UserPresenceRepository presenceRepository;
    private final GroupMemberRepository  memberRepository;
    private final UserRepository         userRepository;
    private final SimpMessagingTemplate  messagingTemplate;

    public PresenceServiceImpl(UserPresenceRepository presenceRepository,
                               GroupMemberRepository memberRepository,
                               UserRepository userRepository,
                               SimpMessagingTemplate messagingTemplate) {
        this.presenceRepository = presenceRepository;
        this.memberRepository   = memberRepository;
        this.userRepository     = userRepository;
        this.messagingTemplate  = messagingTemplate;
    }

    @Override
    public void userConnected(Long userId) {
        onlineUsers.add(userId);
        broadcastPresence(userId, true);
    }

    @Override
    public void userDisconnected(Long userId) {
        onlineUsers.remove(userId);

        LocalDateTime now = LocalDateTime.now();
        userRepository.findById(userId).ifPresent(user -> {
            UserPresence presence = presenceRepository.findById(userId)
                    .orElseGet(() -> {
                        UserPresence p = new UserPresence();
                        p.setUser(user);
                        return p;
                    });
            presence.setLastSeen(now);
            presenceRepository.save(presence);
        });

        broadcastPresence(userId, false);
    }

    @Override
    public boolean isOnline(Long userId) {
        return onlineUsers.contains(userId);
    }

    @Override
    public LocalDateTime getLastSeen(Long userId) {
        return presenceRepository.findById(userId)
                .map(UserPresence::getLastSeen)
                .orElse(null);
    }

    private void broadcastPresence(Long userId, boolean online) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        LocalDateTime lastSeen = online ? null : getLastSeen(userId);
        PresenceEventDTO event = new PresenceEventDTO(
                userId, user.getName(), online, lastSeen);

        memberRepository.findByUserUserId(userId).forEach(member ->
            messagingTemplate.convertAndSend(
                "/topic/presence." + member.getGroup().getGroupId(), event)
        );
    }
}