package eafit.gruopChat.group.dto;

import java.time.LocalDateTime;

import eafit.gruopChat.shared.enums.InvitationStatus;

public record InvitationResponseDTO(
        Long invitationId,
        Long groupId,
        String groupName,
        Long invitedByUserId,
        String invitedByName,
        Long invitedUserId,
        String invitedUserName,
        InvitationStatus status,
        LocalDateTime sentAt,
        LocalDateTime respondedAt
) {}