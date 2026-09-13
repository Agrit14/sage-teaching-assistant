package com.sage.teachingassistant.workflow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A stage that records every context it is handed and returns predictable output.
 *
 * <p>Output includes what it saw, so tests can assert on the data that actually
 * travelled between stages rather than only on the reply text.
 */
class StubStage implements WorkflowStage {

    private final String workflowKey;
    private final String key;
    private final String name;

    /** Every context this stage has been executed with, in order. */
    final List<StageContext> received = new ArrayList<>();

    StubStage(String workflowKey, String key, String name) {
        this.workflowKey = workflowKey;
        this.key = key;
        this.name = name;
    }

    @Override
    public String workflowKey() {
        return workflowKey;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public StageResult execute(StageContext context) {
        received.add(context);

        Map<String, String> output = new LinkedHashMap<>();
        output.put(key + ".attempt", String.valueOf(context.attempt()));
        output.put(key + ".sawPrior", String.join("|", context.priorOutputs().keySet()));
        if (context.feedback() != null) {
            output.put(key + ".feedback", context.feedback());
        }
        return StageResult.of(key + " ran", output);
    }

    StageContext lastContext() {
        return received.get(received.size() - 1);
    }
}
