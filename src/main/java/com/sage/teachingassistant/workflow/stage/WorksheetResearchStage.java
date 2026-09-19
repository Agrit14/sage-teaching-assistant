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
public class WorksheetResearchStage implements WorkflowStage {

    public static final String WORKFLOW_KEY = "worksheet-generation";
    public static final String STAGE_KEY = "worksheet-research";

    private final EducationalContentService educationalService;

    public WorksheetResearchStage(EducationalContentService educationalService) {
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
        return "Worksheet Topic & Curriculum Research";
    }

    @Override
    public StageResult execute(StageContext context) {
        String userInput = context.latestUserInput() != null ? context.latestUserInput().strip() : "";
        String priorRequest = context.priorOutput(WorkflowPayload.CONFIRMED_REQUEST);
        String activeTopic = priorRequest != null && !priorRequest.isBlank() ? priorRequest : userInput;

        String outline = educationalService.researchWorksheetOutline(activeTopic, context.feedback());

        String message = """
                I've researched the curriculum and prepared the proposed Worksheet Outline for:
                "%s"

                ---
                %s
                ---

                Is this right?
                • Say "yes" to proceed and generate the Word document.
                • Or reply with what you'd like to adjust (e.g., "add 5 more numerical questions", "make it for Class 10 CBSE").""".formatted(
                activeTopic, outline);

        Map<String, String> output = new LinkedHashMap<>();
        output.put(WorkflowPayload.CONFIRMED_REQUEST, activeTopic);
        output.put(WorkflowPayload.CONFIRMED_OUTLINE, outline);
        output.put("worksheet.attempt", String.valueOf(context.attempt()));

        return StageResult.of(message, output);
    }
}
