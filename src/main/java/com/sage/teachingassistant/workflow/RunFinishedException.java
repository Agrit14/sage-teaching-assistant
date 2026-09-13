package com.sage.teachingassistant.workflow;

import com.sage.teachingassistant.domain.RunStatus;

/** Thrown when a message arrives for a run that has already finished. */
public class RunFinishedException extends RuntimeException {

    public RunFinishedException(String runId, RunStatus status) {
        super("Workflow run " + runId + " is already " + status);
    }
}
