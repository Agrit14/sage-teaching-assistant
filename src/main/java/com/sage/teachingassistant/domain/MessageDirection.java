package com.sage.teachingassistant.domain;

/** Which way a message travelled. */
public enum MessageDirection {
    /** From the learner to Sage. */
    INBOUND,
    /** From Sage to the learner. */
    OUTBOUND
}
