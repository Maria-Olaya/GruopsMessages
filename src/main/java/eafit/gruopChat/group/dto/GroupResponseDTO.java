package eafit.gruopChat.group.dto;

import java.time.LocalDateTime;

public record GroupResponseDTO(
        Long groupId,
        String name,
        String description,
        Long createdByUserId,
        String createdByName,
        boolean isPrivate,
        int memberCount,
        int channelCount,
        LocalDateTime createdAt
) {}