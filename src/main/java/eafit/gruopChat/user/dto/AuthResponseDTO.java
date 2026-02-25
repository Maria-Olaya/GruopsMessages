package eafit.gruopChat.user.dto;

import eafit.gruopChat.shared.enums.Role;

public record AuthResponseDTO(
        String token,
        Long userId,
        String name,
        Role role,
        long expiresIn
) {}