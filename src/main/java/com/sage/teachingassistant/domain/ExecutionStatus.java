package com.sage.teachingassistant.domain;

/** How a single stage execution ended. */
public enum ExecutionStatus {
    /** The stage ran and produced output; the user has not answered yet. */
    EXECUTED,
    /** The user approved this output. */
    APPROVED,
    /** The user asked for changes; the stage ran again. */
    REVISED
}
