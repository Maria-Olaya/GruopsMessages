package eafit.gruopChat.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChannelRequestDTO(

        @NotBlank(message = "Channel name is required")
        @Size(max = 100)
        String name,

        @Size(max = 255)
        String description
) {}