package com.sage.teachingassistant.workflow;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Approves only when the message is, on its own, an unambiguous yes.
 *
 * <p>Deliberately strict. Anything that is not a plain confirmation — including
 * "yes but add more on mitosis" — is treated as feedback and sends the stage back
 * around. Being wrong in that direction costs one extra round trip; being wrong
 * the other way silently skips work the user wanted changed.
 *
 * <p>Replace with a model-backed classifier when the tools are chosen; nothing
 * outside this class needs to change.
 */
@Component
public class KeywordApprovalClassifier implements ApprovalClassifier {

    private static final Set<String> APPROVALS = Set.of(
            "yes", "y", "ye", "yep", "yeah", "yup", "yah",
            "ok", "okay", "k", "kk", "alright", "all right",
            "sure", "fine", "good", "great", "perfect", "nice", "excellent",
            "correct", "right", "exactly", "accurate", "that's right", "thats right",
            "approve", "approved", "confirm", "confirmed", "accept", "accepted",
            "go ahead", "go on", "proceed", "continue", "next", "move on",
            "looks good", "look good", "lgtm", "looks great", "looks fine",
            "satisfied", "happy", "done", "all good", "no changes", "no change"
    );

    private static final Pattern TRAILING_PUNCTUATION = Pattern.compile("[.!,\\s]+$");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    @Override
    public boolean isApproval(String message) {
        if (message == null) {
            return false;
        }

        String normalised = message.strip().toLowerCase(Locale.ROOT);
        normalised = WHITESPACE.matcher(normalised).replaceAll(" ");
        normalised = TRAILING_PUNCTUATION.matcher(normalised).replaceAll("");

        return APPROVALS.contains(normalised);
    }
}
