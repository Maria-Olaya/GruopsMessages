package eafit.gruopChat.user.dto;

import java.time.LocalDateTime;

import eafit.gruopChat.shared.enums.Role;

public record UserResponseDTO(
        Long userId,
        String name,
        String email,
        Role role,
        boolean enabled,
        LocalDateTime createdAt,
        String profilePictureUrl,
        String phoneNumber
) {}