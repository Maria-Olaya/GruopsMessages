package eafit.gruopChat.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GroupRequestDTO(

        @NotBlank(message = "Group name is required")
        @Size(max = 100)
        String name,

        @Size(max = 255)
        String description,

        @NotNull
        boolean isPrivate
) {}