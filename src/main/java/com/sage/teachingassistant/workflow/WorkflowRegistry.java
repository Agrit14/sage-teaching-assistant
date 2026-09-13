package com.sage.teachingassistant.workflow;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Holds every workflow the engine knows about, keyed by workflow key. */
@Component
public class WorkflowRegistry {

    private final Map<String, WorkflowDefinition> definitions;

    public WorkflowRegistry(List<WorkflowDefinition> definitions) {
        Map<String, WorkflowDefinition> byKey = new LinkedHashMap<>();
        for (WorkflowDefinition definition : definitions) {
            WorkflowDefinition clash = byKey.put(definition.key(), definition);
            if (clash != null) {
                throw new IllegalStateException(
                        "Two workflows share the key '" + definition.key() + "'");
            }
        }
        this.definitions = Map.copyOf(byKey);
    }

    public WorkflowDefinition require(String workflowKey) {
        WorkflowDefinition definition = definitions.get(workflowKey);
        if (definition == null) {
            throw new WorkflowNotFoundException(workflowKey, definitions.keySet());
        }
        return definition;
    }

    public Collection<WorkflowDefinition> all() {
        return definitions.values();
    }
}
