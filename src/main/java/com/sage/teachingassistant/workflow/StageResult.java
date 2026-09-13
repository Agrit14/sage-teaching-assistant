package com.sage.teachingassistant.workflow;

import java.util.Map;

/**
 * What a stage produces.
 *
 * <p>Every stage pauses for approval, so there is no flag for it — the engine
 * always stops here and waits for the user's verdict.
 *
 * @param message what the user is shown
 * @param output  values handed to the next stage. Keys should be namespaced per
 *                stage (for example {@code research.documentPath}) so that two
 *                stages writing the same key cannot silently clobber each other.
 */
public record StageResult(String message, Map<String, String> output) {

    public static StageResult of(String message, Map<String, String> output) {
        return new StageResult(message, Map.copyOf(output));
    }
}
