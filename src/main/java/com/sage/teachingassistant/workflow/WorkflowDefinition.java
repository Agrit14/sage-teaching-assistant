package com.sage.teachingassistant.workflow;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** A named, ordered sequence of stages. */
public record WorkflowDefinition(String key, String name, List<WorkflowStage> stages) {

    public WorkflowDefinition {
        stages = List.copyOf(stages);
        if (stages.isEmpty()) {
            throw new IllegalArgumentException("Workflow '" + key + "' has no stages");
        }

        Set<String> seen = new HashSet<>();
        for (WorkflowStage stage : stages) {
            if (!key.equals(stage.workflowKey())) {
                throw new IllegalArgumentException(
                        "Stage '" + stage.key() + "' declares workflow '" + stage.workflowKey()
                                + "' but was registered under '" + key + "'");
            }
            if (!seen.add(stage.key())) {
                throw new IllegalArgumentException(
                        "Workflow '" + key + "' has two stages keyed '" + stage.key() + "'");
            }
        }
    }

    public int stageCount() {
        return stages.size();
    }

    public WorkflowStage stageAt(int index) {
        if (index < 0 || index >= stages.size()) {
            throw new IllegalArgumentException(
                    "Stage index " + index + " is out of range for workflow '" + key + "'");
        }
        return stages.get(index);
    }

    /** The stage after {@code index}, or empty when this was the last one. */
    public Optional<WorkflowStage> stageAfter(int index) {
        int next = index + 1;
        return next < stages.size() ? Optional.of(stages.get(next)) : Optional.empty();
    }
}
