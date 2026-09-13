package com.sage.teachingassistant.api.dto;

import com.sage.teachingassistant.workflow.WorkflowDefinition;
import com.sage.teachingassistant.workflow.WorkflowStage;

import java.util.List;
import java.util.stream.IntStream;

/** Describes an available workflow so the caller knows what it can start. */
public record WorkflowSummaryResponse(
        String key,
        String name,
        int stageCount,
        List<StageSummary> stages
) {

    public record StageSummary(int index, String key, String name) {
    }

    public static WorkflowSummaryResponse from(WorkflowDefinition definition) {
        List<StageSummary> stages = IntStream.range(0, definition.stageCount())
                .mapToObj(index -> {
                    WorkflowStage stage = definition.stageAt(index);
                    return new StageSummary(index, stage.key(), stage.name());
                })
                .toList();

        return new WorkflowSummaryResponse(
                definition.key(), definition.name(), definition.stageCount(), stages);
    }
}
