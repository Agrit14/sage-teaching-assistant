package com.sage.teachingassistant.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sage.teachingassistant.ai.EducationalContentService;
import com.sage.teachingassistant.ai.GeminiClient;
import com.sage.teachingassistant.api.dto.RunMessageRequest;
import com.sage.teachingassistant.api.dto.RunResponse;
import com.sage.teachingassistant.api.dto.StartRunRequest;
import com.sage.teachingassistant.document.FileStorageService;
import com.sage.teachingassistant.domain.RunStatus;
import com.sage.teachingassistant.domain.StageExecution;
import com.sage.teachingassistant.domain.WorkflowRun;
import com.sage.teachingassistant.repository.StageExecutionRepository;
import com.sage.teachingassistant.repository.WorkflowRunRepository;
import com.sage.teachingassistant.workflow.KeywordApprovalClassifier;
import com.sage.teachingassistant.workflow.StageContext;
import com.sage.teachingassistant.workflow.StageResult;
import com.sage.teachingassistant.workflow.WorkflowDefinition;
import com.sage.teachingassistant.workflow.WorkflowEngine;
import com.sage.teachingassistant.workflow.WorkflowRegistry;
import com.sage.teachingassistant.workflow.WorkflowStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkflowRoutingTest {

    private final Map<String, WorkflowRun> runs = new HashMap<>();
    private final List<StageExecution> executions = new ArrayList<>();

    private WorkflowController controller;
    private EducationalContentService eduService;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        runs.clear();
        executions.clear();

        ObjectMapper mapper = new ObjectMapper();
        GeminiClient geminiClient = new GeminiClient("", "", "", mapper);
        eduService = new EducationalContentService(geminiClient);
        FileStorageService storageService = new FileStorageService(tempDir.toString());

        WorkflowRunRepository runRepository = mock(WorkflowRunRepository.class);
        StageExecutionRepository executionRepository = mock(StageExecutionRepository.class);

        when(runRepository.save(any(WorkflowRun.class))).thenAnswer(inv -> {
            WorkflowRun r = inv.getArgument(0);
            runs.put(r.getId(), r);
            return r;
        });
        when(runRepository.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(runs.get(inv.getArgument(0))));

        when(executionRepository.save(any(StageExecution.class))).thenAnswer(inv -> {
            StageExecution e = inv.getArgument(0);
            executions.add(e);
            return e;
        });

        // Dummy stage for worksheet workflow
        WorkflowStage stubStage = new WorkflowStage() {
            @Override public String workflowKey() { return "worksheet-generation"; }
            @Override public String key() { return "worksheet-research"; }
            @Override public String name() { return "Research Stage"; }
            @Override public StageResult execute(StageContext context) {
                return StageResult.of("Research Outline Proposal", Map.of("outline", "Sample Outline"));
            }
        };

        WorkflowDefinition worksheetDef = new WorkflowDefinition(
                "worksheet-generation", "Worksheet", List.of(stubStage));
        WorkflowRegistry registry = new WorkflowRegistry(List.of(worksheetDef));

        WorkflowEngine engine = new WorkflowEngine(
                registry, runRepository, executionRepository,
                new KeywordApprovalClassifier(), mapper);

        controller = new WorkflowController(
                engine, registry, storageService, eduService,
                runRepository, executionRepository, mapper);
    }

    @Test
    void testGeneralQuestionWithoutContextRoutesDirectlyToLlm() {
        // User asks a general question without selecting an option or class/chapter
        StartRunRequest request = new StartRunRequest(
                null,
                "What is Newton's third law of motion?",
                null,
                null,
                null);

        RunResponse response = controller.start(request);

        assertThat(response).isNotNull();
        assertThat(response.workflowKey()).isEqualTo("general-qa");
        assertThat(response.status()).isEqualTo(RunStatus.COMPLETED);
        assertThat(response.completed()).isTrue();
        assertThat(response.canApprove()).isFalse();
        assertThat(response.message()).contains("Here is the answer to your question");
    }

    @Test
    void testDocumentQueryWithOptionRoutesThroughProcess() {
        // User selects worksheet-generation and provides class and chapter
        StartRunRequest request = new StartRunRequest(
                "worksheet-generation",
                null,
                "Class 10",
                "Light - Reflection",
                "Include numericals");

        RunResponse response = controller.start(request);

        assertThat(response).isNotNull();
        assertThat(response.workflowKey()).isEqualTo("worksheet-generation");
        assertThat(response.status()).isEqualTo(RunStatus.AWAITING_APPROVAL);
        assertThat(response.completed()).isFalse();
        assertThat(response.canApprove()).isTrue();
        assertThat(response.stageKey()).isEqualTo("worksheet-research");
        assertThat(response.message()).isEqualTo("Research Outline Proposal");
    }

    @Test
    void testChatEndpointWithGeneralQuestionRoutesDirectly() {
        StartRunRequest chatReq = new StartRunRequest(
                null,
                "Can you explain photosynthesis?",
                null,
                null,
                null);

        RunResponse response = controller.chat(chatReq);

        assertThat(response).isNotNull();
        assertThat(response.workflowKey()).isEqualTo("general-qa");
        assertThat(response.completed()).isTrue();
        assertThat(response.message()).isNotEmpty();
    }

    @Test
    void testFollowUpMessageOnGeneralQaRun() {
        StartRunRequest request = new StartRunRequest(
                null,
                "What is velocity?",
                null,
                null,
                null);

        RunResponse initial = controller.start(request);
        assertThat(initial.completed()).isTrue();

        // Send follow-up question
        RunMessageRequest followUp = new RunMessageRequest("Can you give an example?");
        RunResponse followUpResp = controller.send(initial.runId(), followUp);

        assertThat(followUpResp.completed()).isTrue();
        assertThat(followUpResp.workflowKey()).isEqualTo("general-qa");
        assertThat(followUpResp.message()).contains("Here is the answer to your question");
    }
}
