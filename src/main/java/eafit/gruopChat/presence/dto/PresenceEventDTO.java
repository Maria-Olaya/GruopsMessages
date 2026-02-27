package eafit.gruopChat.presence.dto;

import java.time.LocalDateTime;

public record PresenceEventDTO(
        Long          userId,
        String        userName,
        boolean       online,
        LocalDateTime lastSeen
) {}