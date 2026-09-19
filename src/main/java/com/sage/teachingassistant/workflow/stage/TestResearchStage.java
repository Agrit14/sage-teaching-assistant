package com.sage.teachingassistant.workflow.stage;

import com.sage.teachingassistant.ai.EducationalContentService;
import com.sage.teachingassistant.workflow.StageContext;
import com.sage.teachingassistant.workflow.StageResult;
import com.sage.teachingassistant.workflow.WorkflowPayload;
import com.sage.teachingassistant.workflow.WorkflowStage;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TestResearchStage implements WorkflowStage {

    public static final String WORKFLOW_KEY = "test-generation";
    public static final String STAGE_KEY = "test-research";

    private final EducationalContentService educationalService;

    public TestResearchStage(EducationalContentService educationalService) {
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
        return "Test Paper Blueprint & Syllabus Research";
    }

    @Override
    public StageResult execute(StageContext context) {
        String userInput = context.latestUserInput() != null ? context.latestUserInput().strip() : "";
        String priorRequest = context.priorOutput(WorkflowPayload.CONFIRMED_REQUEST);
        String activeTopic = priorRequest != null && !priorRequest.isBlank() ? priorRequest : userInput;

        String blueprint = educationalService.researchTestBlueprint(activeTopic, context.feedback());

        String message = """
                I've created the Test Paper Examination Blueprint for:
                "%s"

                --- Proposed Blueprint & Mark Distribution ---
                %s
                -----------------------------------------------

                Does this structure meet your examination requirements?
                • Say "yes" to proceed with question paper generation.
                • Or reply with changes (e.g. "change total marks to 50", "add more case-based questions").""".formatted(
                activeTopic, blueprint);

        Map<String, String> output = new LinkedHashMap<>();
        output.put(WorkflowPayload.CONFIRMED_REQUEST, activeTopic);
        output.put(WorkflowPayload.CONFIRMED_OUTLINE, blueprint);
        output.put("test.attempt", String.valueOf(context.attempt()));

        return StageResult.of(message, output);
    }
}
