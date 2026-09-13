package com.sage.teachingassistant.workflow;

/** Thrown when a run id does not exist. */
public class RunNotFoundException extends RuntimeException {

    public RunNotFoundException(String runId) {
        super("No workflow run with id " + runId);
    }
}
