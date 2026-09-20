package com.sage.teachingassistant.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * Sent to an existing run. The message is either an approval ("yes", "approve") or
 * instructions for changing the current stage's output.
 */
public record RunMessageRequest(

        @JsonAlias({"text", "feedback", "action", "reply", "input", "command"})
        String message
) {
    public String resolveMessage() {
        return (message != null && !message.isBlank()) ? message.trim() : "approve";
    }
}
