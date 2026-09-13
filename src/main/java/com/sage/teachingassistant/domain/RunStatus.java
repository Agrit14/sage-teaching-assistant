package com.sage.teachingassistant.domain;

/** Where a run currently stands. */
public enum RunStatus {
    /** A stage has produced output and is waiting for the user to approve it. */
    AWAITING_APPROVAL,
    /** Every stage was approved. */
    COMPLETED,
    /** The user walked away. */
    CANCELLED
}
