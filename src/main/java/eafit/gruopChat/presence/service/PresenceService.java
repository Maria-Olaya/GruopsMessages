package eafit.gruopChat.presence.service;

import java.time.LocalDateTime;

public interface PresenceService {

    void userConnected(Long userId);

    void userDisconnected(Long userId);

    boolean isOnline(Long userId);

    LocalDateTime getLastSeen(Long userId);
}