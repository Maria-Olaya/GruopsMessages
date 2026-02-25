package eafit.gruopChat.group.dto;

import java.time.LocalDateTime;

import eafit.gruopChat.shared.enums.GroupRole;

public record GroupMemberResponseDTO(
        Long userId,
        String name,
        String email,
        GroupRole role,
        LocalDateTime joinedAt
) {}