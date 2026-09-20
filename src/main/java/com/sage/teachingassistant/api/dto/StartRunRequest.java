package com.sage.teachingassistant.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Starts a new run.
 *
 * <p>Supports either a freeform {@code message} or structured fields
 * ({@code className}, {@code chapterName}, {@code additionalDetails})
 * sent from the chatbot/app.
 */
public record StartRunRequest(

        @NotBlank(message = "workflowKey must not be blank")
        String workflowKey,

        String message,
        String className,
        String chapterName,
        String additionalDetails
) {
    /**
     * Resolves the primary message string. If {@code message} is null or blank,
     * builds a descriptive message from the structured topic fields.
     */
    public String resolveMessage() {
        if (message != null && !message.isBlank()) {
            return message.trim();
        }
        StringBuilder sb = new StringBuilder();
        if (className != null && !className.isBlank()) {
            sb.append("Class: ").append(className.trim()).append(" | ");
        }
        if (chapterName != null && !chapterName.isBlank()) {
            sb.append("Chapter: ").append(chapterName.trim()).append(" | ");
        }
        if (additionalDetails != null && !additionalDetails.isBlank()) {
            sb.append("Details: ").append(additionalDetails.trim());
        }
        String res = sb.toString().trim();
        if (res.endsWith("|")) {
            res = res.substring(0, res.length() - 1).trim();
        }
        return !res.isBlank() ? res : "Curriculum Topic";
    }
}
