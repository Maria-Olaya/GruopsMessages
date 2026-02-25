package eafit.gruopChat.group.dto;

import java.time.LocalDateTime;

public record ChannelResponseDTO(
        Long channelId,
        Long groupId,
        String name,
        String description,
        Long createdByUserId,
        String createdByName,
        LocalDateTime createdAt
) {}