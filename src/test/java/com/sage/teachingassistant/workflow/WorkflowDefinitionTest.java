package com.sage.teachingassistant.workflow;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowDefinitionTest {

    private static final String WORKFLOW = "demo";

    @Test
    void exposesStagesInDeclaredOrder() {
        StubStage first = new StubStage(WORKFLOW, "a", "A");
        StubStage second = new StubStage(WORKFLOW, "b", "B");

        WorkflowDefinition definition = new WorkflowDefinition(WORKFLOW, "Demo", List.of(first, second));

        assertThat(definition.stageCount()).isEqualTo(2);
        assertThat(definition.stageAt(0)).isSameAs(first);
        assertThat(definition.stageAt(1)).isSameAs(second);
        assertThat(definition.stageAfter(0)).contains(second);
    }

    @Test
    void hasNoStageAfterTheLast() {
        WorkflowDefinition definition = new WorkflowDefinition(WORKFLOW, "Demo",
                List.of(new StubStage(WORKFLOW, "only", "Only")));

        assertThat(definition.stageAfter(0)).isEmpty();
    }

    @Test
    void rejectsAnEmptyStageList() {
        assertThatThrownBy(() -> new WorkflowDefinition(WORKFLOW, "Demo", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no stages");
    }

    @Test
    void rejectsAStageBelongingToAnotherWorkflow() {
        StubStage stray = new StubStage("something-else", "a", "A");

        assertThatThrownBy(() -> new WorkflowDefinition(WORKFLOW, "Demo", List.of(stray)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("something-else");
    }

    @Test
    void rejectsDuplicateStageKeys() {
        StubStage one = new StubStage(WORKFLOW, "dup", "One");
        StubStage two = new StubStage(WORKFLOW, "dup", "Two");

        assertThatThrownBy(() -> new WorkflowDefinition(WORKFLOW, "Demo", List.of(one, two)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dup");
    }

    @Test
    void rejectsAnOutOfRangeIndex() {
        WorkflowDefinition definition = new WorkflowDefinition(WORKFLOW, "Demo",
                List.of(new StubStage(WORKFLOW, "a", "A")));

        assertThatThrownBy(() -> definition.stageAt(5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> definition.stageAt(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registryRejectsDuplicateWorkflowKeys() {
        WorkflowDefinition one = new WorkflowDefinition(WORKFLOW, "One",
                List.of(new StubStage(WORKFLOW, "a", "A")));
        WorkflowDefinition two = new WorkflowDefinition(WORKFLOW, "Two",
                List.of(new StubStage(WORKFLOW, "b", "B")));

        assertThatThrownBy(() -> new WorkflowRegistry(List.of(one, two)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(WORKFLOW);
    }

    @Test
    void registryRejectsAnUnknownKey() {
        WorkflowRegistry registry = new WorkflowRegistry(List.of(
                new WorkflowDefinition(WORKFLOW, "Demo", List.of(new StubStage(WORKFLOW, "a", "A")))));

        assertThatThrownBy(() -> registry.require("nope"))
                .isInstanceOf(WorkflowNotFoundException.class)
                .hasMessageContaining("nope")
                .hasMessageContaining(WORKFLOW);
    }

    @Test
    void registryResolvesAliases() {
        WorkflowDefinition ws = new WorkflowDefinition("worksheet-generation", "Worksheet", List.of(new StubStage("worksheet-generation", "ws", "WS")));
        WorkflowDefinition test = new WorkflowDefinition("test-generation", "Test", List.of(new StubStage("test-generation", "t", "T")));
        WorkflowDefinition notes = new WorkflowDefinition("notes-generation", "Notes", List.of(new StubStage("notes-generation", "n", "N")));
        WorkflowDefinition pdf = new WorkflowDefinition("pdf-print", "PDF", List.of(new StubStage("pdf-print", "p", "P")));

        WorkflowRegistry registry = new WorkflowRegistry(List.of(ws, test, notes, pdf));

        assertThat(registry.require("worksheet")).isSameAs(ws);
        assertThat(registry.require("test")).isSameAs(test);
        assertThat(registry.require("notes")).isSameAs(notes);
        assertThat(registry.require("pdf")).isSameAs(pdf);
    }
}

