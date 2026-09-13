package com.sage.teachingassistant.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request body for pushing a message out to a Telegram chat. */
public record SendMessageRequest(

        @NotNull(message = "chatId is required")
        Long chatId,

        @NotBlank(message = "text must not be blank")
        @Size(max = 4096, message = "text must be at most 4096 characters")
        String text
) {
}
