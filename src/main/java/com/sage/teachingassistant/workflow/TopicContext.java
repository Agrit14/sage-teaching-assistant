package com.sage.teachingassistant.workflow;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encapsulates the class name, chapter name, and additional details
 * provided by the user/chatbot when starting or running an educational workflow.
 */
public record TopicContext(
        String className,
        String chapterName,
        String additionalDetails
) {

    private static final Pattern CLASS_PATTERN = Pattern.compile("(?i)\\b(class|grade|standard)\\s*(\\d+|[ivxlcdm]+|[a-z]+)", Pattern.CASE_INSENSITIVE);

    public static TopicContext resolve(StageContext context) {
        String cls = context.priorOutput(WorkflowPayload.TOPIC_CLASS);
        String ch = context.priorOutput(WorkflowPayload.TOPIC_CHAPTER);
        String details = context.priorOutput(WorkflowPayload.TOPIC_DETAILS);

        if (cls != null && !cls.isBlank() && ch != null && !ch.isBlank()) {
            return new TopicContext(cls.trim(), ch.trim(), details != null ? details.trim() : "");
        }

        String input = context.latestUserInput() != null ? context.latestUserInput().trim() : "";
        String priorRequest = context.priorOutput(WorkflowPayload.CONFIRMED_REQUEST);
        String source = !input.isBlank() ? input : (priorRequest != null ? priorRequest : "");

        // If source has pipe format: Class: X | Chapter: Y | Details: Z
        if (source.contains("|")) {
            String parsedClass = "";
            String parsedChapter = "";
            String parsedDetails = "";
            for (String part : source.split("\\|")) {
                String p = part.trim();
                String pUpper = p.toUpperCase();
                if (pUpper.startsWith("CLASS:")) {
                    parsedClass = p.substring(6).trim();
                } else if (pUpper.startsWith("CHAPTER:")) {
                    parsedChapter = p.substring(8).trim();
                } else if (pUpper.startsWith("DETAILS:") || pUpper.startsWith("DETAIL:")) {
                    parsedDetails = p.substring(p.indexOf(':') + 1).trim();
                } else if (parsedChapter.isEmpty()) {
                    parsedChapter = p;
                }
            }
            if (!parsedChapter.isEmpty() || !parsedClass.isEmpty()) {
                return new TopicContext(
                        !parsedClass.isEmpty() ? parsedClass : (cls != null ? cls : "Class 10"),
                        !parsedChapter.isEmpty() ? parsedChapter : source,
                        !parsedDetails.isEmpty() ? parsedDetails : (details != null ? details : ""));
            }
        }

        // Try extracting Class via Regex
        String detectedClass = cls != null && !cls.isBlank() ? cls : "Class 10";
        Matcher m = CLASS_PATTERN.matcher(source);
        if (m.find()) {
            detectedClass = "Class " + m.group(2).trim();
        }

        String detectedChapter = ch != null && !ch.isBlank() ? ch : source;
        return new TopicContext(detectedClass, detectedChapter, details != null ? details : "");
    }

    public String formattedTitle() {
        StringBuilder sb = new StringBuilder();
        if (className != null && !className.isBlank()) {
            sb.append(className.trim());
        }
        if (chapterName != null && !chapterName.isBlank()) {
            if (!sb.isEmpty()) sb.append(" - ");
            sb.append(chapterName.trim());
        }
        if (additionalDetails != null && !additionalDetails.isBlank()) {
            if (!sb.isEmpty()) sb.append(" (").append(additionalDetails.trim()).append(")");
        }
        return !sb.isEmpty() ? sb.toString() : "Curriculum Unit";
    }
}
