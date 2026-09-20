package com.sage.teachingassistant.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * Starts a new run or routes a general Q&A request.
 *
 * <p>Supports either a freeform {@code message} or structured fields
 * ({@code className}, {@code chapterName}, {@code additionalDetails})
 * sent from the chatbot/app.
 */
public record StartRunRequest(

        @JsonAlias({"workflow_key", "workflow", "type", "workflowId", "option", "selection"})
        String workflowKey,

        @JsonAlias({"prompt", "query", "text", "description"})
        String message,

        @JsonAlias({"class_name", "className", "class", "grade", "standard"})
        String className,

        @JsonAlias({"chapter_name", "chapterName", "chapter", "topic", "subject_topic"})
        String chapterName,

        @JsonAlias({"additional_details", "additionalDetails", "details", "context", "notes", "additionalContext"})
        String additionalDetails
) {
    /**
     * Determines whether the request is a real document generation query.
     * It is considered a document query if an option is selected (e.g., worksheet, test, notes, or pdf-print)
     * or if structured class and chapter context are both supplied.
     */
    public boolean isRealDocumentQuery() {
        if (workflowKey != null && !workflowKey.isBlank()) {
            String key = workflowKey.trim().toLowerCase();
            if (key.contains("worksheet")
                    || key.contains("test")
                    || key.contains("exam")
                    || key.contains("notes")
                    || key.contains("revision")
                    || key.contains("pdf-print")
                    || key.equals("educational-notes")) {
                return true;
            }
        }

        // Real query when class AND chapter are explicitly selected
        return (className != null && !className.isBlank())
                && (chapterName != null && !chapterName.isBlank());
    }

    /**
     * Resolves the target workflow key, defaulting to "worksheet-generation" if class & chapter
     * were given without an explicit option.
     */
    public String resolveWorkflowKey() {
        if (workflowKey != null && !workflowKey.isBlank()) {
            String key = workflowKey.trim().toLowerCase();
            if (key.contains("worksheet")) return "worksheet-generation";
            if (key.contains("test") || key.contains("exam")) return "test-generation";
            if (key.contains("notes") || key.contains("revision")) return "notes-generation";
            if (key.contains("print") || key.contains("pdf")) return "pdf-print";
            return workflowKey.trim();
        }
        return "worksheet-generation";
    }

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

    /**
     * Resolves the freeform question text for general Q&A.
     */
    public String resolveQuestion() {
        if (message != null && !message.isBlank()) {
            return message.trim();
        }
        if (additionalDetails != null && !additionalDetails.isBlank()) {
            return additionalDetails.trim();
        }
        if (chapterName != null && !chapterName.isBlank()) {
            return "Please explain: " + chapterName.trim();
        }
        return "Hello! How can you assist me with my studies?";
    }
}
