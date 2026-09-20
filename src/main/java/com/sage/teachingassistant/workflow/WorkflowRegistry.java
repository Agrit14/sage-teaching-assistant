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
        if (workflowKey == null) {
            throw new WorkflowNotFoundException("null", definitions.keySet());
        }
        String trimmed = workflowKey.trim();
        WorkflowDefinition exact = definitions.get(trimmed);
        if (exact != null) {
            return exact;
        }
        String normalized = normalizeKey(trimmed);
        WorkflowDefinition aliased = definitions.get(normalized);
        if (aliased != null) {
            return aliased;
        }
        throw new WorkflowNotFoundException(workflowKey, definitions.keySet());
    }

    private static String normalizeKey(String key) {
        String k = key.toLowerCase().replace('_', '-').replace(' ', '-');
        if (k.equals("worksheet") || k.startsWith("worksheet")) {
            return "worksheet-generation";
        }
        if (k.equals("test") || k.equals("exam") || k.startsWith("test")) {
            return "test-generation";
        }
        if (k.equals("notes") || k.equals("revision") || k.startsWith("note")) {
            return "notes-generation";
        }
        if (k.equals("pdf") || k.equals("print") || k.contains("pdf")) {
            return "pdf-print";
        }
        return k;
    }

    public Collection<WorkflowDefinition> all() {
        return definitions.values();
    }
}
