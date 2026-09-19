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
public class NotesResearchStage implements WorkflowStage {

    public static final String WORKFLOW_KEY = "notes-generation";
    public static final String STAGE_KEY = "notes-research";

    private final EducationalContentService educationalService;

    public NotesResearchStage(EducationalContentService educationalService) {
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
        return "Revision Notes Topic & Concepts Research";
    }

    @Override
    public StageResult execute(StageContext context) {
        String userInput = context.latestUserInput() != null ? context.latestUserInput().strip() : "";
        String priorRequest = context.priorOutput(WorkflowPayload.CONFIRMED_REQUEST);
        String activeTopic = priorRequest != null && !priorRequest.isBlank() ? priorRequest : userInput;

        String outline = educationalService.researchNotesOutline(activeTopic, context.feedback());

        String message = """
                I've researched the curriculum and prepared the Revision Notes Outline for:
                "%s"

                --- Proposed Notes Structure & Key Concepts ---
                %s
                ------------------------------------------------

                Is this the scope you want for the revision notes?
                • Say "yes" to generate the comprehensive Word notes document.
                • Or tell me what to emphasize (e.g. "include more real-life examples and memory mnemonics").""".formatted(
                activeTopic, outline);

        Map<String, String> output = new LinkedHashMap<>();
        output.put(WorkflowPayload.CONFIRMED_REQUEST, activeTopic);
        output.put(WorkflowPayload.CONFIRMED_OUTLINE, outline);
        output.put("notes.attempt", String.valueOf(context.attempt()));

        return StageResult.of(message, output);
    }
}
