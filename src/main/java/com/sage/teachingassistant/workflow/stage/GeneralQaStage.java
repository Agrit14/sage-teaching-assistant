package com.sage.teachingassistant.workflow.stage;

import com.sage.teachingassistant.ai.EducationalContentService;
import com.sage.teachingassistant.workflow.StageContext;
import com.sage.teachingassistant.workflow.StageResult;
import com.sage.teachingassistant.workflow.WorkflowStage;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Executes a direct single-turn LLM response for general questions without requiring
 * multi-stage document generation overhead.
 */
@Component
public class GeneralQaStage implements WorkflowStage {

    public static final String WORKFLOW_KEY = "general-qa";
    public static final String STAGE_KEY = "direct-answer";

    private final EducationalContentService educationalService;

    public GeneralQaStage(EducationalContentService educationalService) {
        this.educationalService = educationalService;
    }

    @Override
    public String workflowKey() {
        return WORKFLOW_KEY;
    }

    @Override
    public String key() {
        return STAGE_KEY;
    }

    @Override
    public String name() {
        return "Direct Educational Q&A";
    }

    @Override
    public StageResult execute(StageContext context) {
        String query = context.latestUserInput();
        if (query == null || query.isBlank()) {
            query = context.priorOutput("query");
        }
        if (query == null || query.isBlank()) {
            query = "Hello, how can I assist you with your studies today?";
        }

        String answer = educationalService.answerGeneralQuestion(query);
        return StageResult.of(answer, Map.of("query", query, "answer", answer, "type", "direct_llm_response"));
    }
}
