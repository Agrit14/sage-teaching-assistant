package com.sage.teachingassistant.workflow;

import java.util.Collection;

/** Thrown when a workflow key is not one the engine knows about. */
public class WorkflowNotFoundException extends RuntimeException {

    public WorkflowNotFoundException(String workflowKey, Collection<String> known) {
        super("Unknown workflow '" + workflowKey + "'. Known workflows: " + String.join(", ", known));
    }
}
