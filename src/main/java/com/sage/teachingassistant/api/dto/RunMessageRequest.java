package com.sage.teachingassistant.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Sent to an existing run. The message is either an approval ("yes") or
 * instructions for changing the current stage's output.
 */
public record RunMessageRequest(

        @NotBlank(message = "message must not be blank")
        String message
) {
}
