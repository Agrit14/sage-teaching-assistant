package com.sage.teachingassistant.improvement;

import com.sage.teachingassistant.ai.EducationalContentService;
import com.sage.teachingassistant.ai.GeminiClient;
import com.sage.teachingassistant.api.WorkflowController;
import com.sage.teachingassistant.api.dto.FeedbackRequest;
import com.sage.teachingassistant.api.dto.ImprovementRuleResponse;
import com.sage.teachingassistant.document.FileStorageService;
import com.sage.teachingassistant.domain.ImprovementRule;
import com.sage.teachingassistant.repository.ImprovementRuleRepository;
import com.sage.teachingassistant.repository.StageExecutionRepository;
import com.sage.teachingassistant.repository.WorkflowRunRepository;
import com.sage.teachingassistant.workflow.KeywordApprovalClassifier;
import com.sage.teachingassistant.workflow.WorkflowEngine;
import com.sage.teachingassistant.workflow.WorkflowRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImprovementRuleTest {

    private final List<ImprovementRule> inMemoryRules = new ArrayList<>();
    private ImprovementRuleRepository ruleRepository;
    private ImprovementRuleService improvementRuleService;
    private GeminiClient geminiClient;
    private EducationalContentService eduService;
    private WorkflowController controller;

    @BeforeEach
    void setUp() {
        inMemoryRules.clear();
        ruleRepository = mock(ImprovementRuleRepository.class);

        when(ruleRepository.save(any(ImprovementRule.class))).thenAnswer(inv -> {
            ImprovementRule rule = inv.getArgument(0);
            if (rule.getId() == null) {
                // emulate ID generation
                try {
                    var field = ImprovementRule.class.getDeclaredField("id");
                    field.setAccessible(true);
                    field.set(rule, (long) (inMemoryRules.size() + 1));
                } catch (Exception ignored) {}
            }
            inMemoryRules.add(rule);
            return rule;
        });

        when(ruleRepository.findByStageKeyInAndIsActiveTrueOrderByCreatedAtAsc(any())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            List<String> keys = inv.getArgument(0);
            return inMemoryRules.stream()
                    .filter(r -> r.isActive() && keys.stream().anyMatch(k -> k.equalsIgnoreCase(r.getStageKey())))
                    .toList();
        });

        when(ruleRepository.findByIsActiveTrueOrderByCreatedAtDesc()).thenAnswer(inv ->
                inMemoryRules.stream().filter(ImprovementRule::isActive).toList());

        when(ruleRepository.findByStageKeyOrderByCreatedAtDesc(anyString())).thenAnswer(inv -> {
            String stageKey = inv.getArgument(0);
            return inMemoryRules.stream()
                    .filter(r -> stageKey.equalsIgnoreCase(r.getStageKey()))
                    .toList();
        });

        when(ruleRepository.findById(any())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return inMemoryRules.stream().filter(r -> id.equals(r.getId())).findFirst();
        });

        improvementRuleService = new ImprovementRuleService(ruleRepository);

        geminiClient = mock(GeminiClient.class);
        when(geminiClient.generate(anyString(), anyString(), anyBoolean())).thenReturn("Generated response");

        eduService = new EducationalContentService(geminiClient, improvementRuleService);

        WorkflowRunRepository runRepository = mock(WorkflowRunRepository.class);
        StageExecutionRepository execRepository = mock(StageExecutionRepository.class);
        WorkflowRegistry registry = new WorkflowRegistry(List.of());
        WorkflowEngine engine = new WorkflowEngine(registry, runRepository, execRepository, new KeywordApprovalClassifier(), new ObjectMapper());
        FileStorageService storageService = mock(FileStorageService.class);

        controller = new WorkflowController(
                engine, registry, storageService, eduService,
                runRepository, execRepository, new ObjectMapper(), improvementRuleService);
    }

    @Test
    void testRecordFeedbackAndBuildPromptForSpecificStage() {
        FeedbackRequest request = new FeedbackRequest(
                "run-123",
                "worksheet-draft",
                "worksheet-generator",
                "Always add marks like [2 Marks] for conceptual questions",
                "formatting"
        );

        ImprovementRuleResponse response = improvementRuleService.recordFeedback(request);
        assertThat(response).isNotNull();
        assertThat(response.stageKey()).isEqualTo("worksheet-draft");
        assertThat(response.ruleText()).isEqualTo("Always add marks like [2 Marks] for conceptual questions");
        assertThat(response.active()).isTrue();

        // Prompt for worksheet-draft should contain the rule
        String prompt = improvementRuleService.buildRulePrompt("worksheet-draft");
        assertThat(prompt).contains("### CONTINUOUS IMPROVEMENT RULES & USER PREFERENCES");
        assertThat(prompt).contains("Always add marks like [2 Marks] for conceptual questions");

        // Prompt for unrelated stage (e.g., test-research) should not contain stage-specific rule
        String testPrompt = improvementRuleService.buildRulePrompt("test-research");
        assertThat(testPrompt).isEmpty();
    }

    @Test
    void testGlobalFeedbackAppliesToAllStages() {
        // Feedback with null or "ALL" stageKey
        FeedbackRequest globalRequest = new FeedbackRequest(
                null,
                null, // defaults to "ALL"
                null,
                "Tone should always be encouraging and academic",
                "tone"
        );

        ImprovementRuleResponse response = improvementRuleService.recordFeedback(globalRequest);
        assertThat(response.stageKey()).isEqualTo("ALL");

        // Should appear in worksheet-draft prompt
        String wsPrompt = improvementRuleService.buildRulePrompt("worksheet-draft");
        assertThat(wsPrompt).contains("Tone should always be encouraging and academic");

        // Should also appear in direct-answer prompt
        String qaPrompt = improvementRuleService.buildRulePrompt("direct-answer");
        assertThat(qaPrompt).contains("Tone should always be encouraging and academic");
    }

    @Test
    void testEducationalContentServiceInjectsRulesIntoSystemPrompt() {
        // Record a rule for worksheet-draft
        improvementRuleService.recordFeedback(new FeedbackRequest(
                null,
                "worksheet-draft",
                "worksheet-generator",
                "Include real-life applications for physics questions",
                "content"
        ));

        eduService.generateWorksheetContent("Grade 10 - Electricity", "Outline", null);

        ArgumentCaptor<String> sysCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiClient).generate(sysCaptor.capture(), promptCaptor.capture(), anyBoolean());

        String capturedSys = sysCaptor.getValue();
        assertThat(capturedSys).contains("Include real-life applications for physics questions");
    }

    @Test
    void testDirectAnswerInjectsRulesIntoSystemPrompt() {
        improvementRuleService.recordFeedback(new FeedbackRequest(
                null,
                "direct-answer",
                null,
                "Explain formulas step by step with SI units",
                "explanations"
        ));

        eduService.answerGeneralQuestion("What is momentum?");

        ArgumentCaptor<String> sysCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiClient).generate(sysCaptor.capture(), anyString(), anyBoolean());

        String capturedSys = sysCaptor.getValue();
        assertThat(capturedSys).contains("Explain formulas step by step with SI units");
    }

    @Test
    void testWorkflowControllerFeedbackEndpoints() {
        FeedbackRequest req = new FeedbackRequest(
                "run-456",
                "test-draft",
                "test-creator",
                "Ensure Section B has at least 3 numerical problems",
                "structure"
        );

        ImprovementRuleResponse saved = controller.submitFeedback(req);
        assertThat(saved.ruleText()).isEqualTo("Ensure Section B has at least 3 numerical problems");

        List<ImprovementRuleResponse> stageRules = controller.listFeedbackRules("test-draft");
        assertThat(stageRules).hasSize(1);
        assertThat(stageRules.get(0).stageKey()).isEqualTo("test-draft");

        List<ImprovementRuleResponse> allRules = controller.listFeedbackRules(null);
        assertThat(allRules).hasSize(1);
    }
}
