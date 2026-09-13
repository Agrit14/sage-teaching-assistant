package com.sage.teachingassistant.workflow.stage;

import com.sage.teachingassistant.workflow.StageContext;
import com.sage.teachingassistant.workflow.StageResult;
import com.sage.teachingassistant.workflow.WorkflowPayload;
import com.sage.teachingassistant.workflow.WorkflowStage;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stage 2 — research the confirmed request and produce a draft document.
 *
 * <p>Reads the confirmed request from {@link WorkflowPayload#CONFIRMED_REQUEST}
 * and writes a draft plus a summary. On a revision it is handed the user's
 * feedback and runs again, replacing its previous output.
 *
 * <p><b>Tool not yet chosen.</b> The plan discussed was an LLM driven by the
 * confirmed request, producing the material that goes into the Word document.
 */
@Component
public class ResearchStage implements WorkflowStage {

    public static final String WORKFLOW_KEY = "educational-notes";
    public static final String STAGE_KEY = "research";

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
        return "Research and draft";
    }

    @Override
    public StageResult execute(StageContext context) {
        String request = context.priorOutput(WorkflowPayload.CONFIRMED_REQUEST);

        // TODO: stage tool. Search the web for material on the request, then have
        //       the model write it into a Word document and save it somewhere the
        //       next stage can read.

        String revisionNote = context.isRevision()
                ? "\n\nChanges applied: \"" + context.feedback().strip() + "\""
                : "";

        String message = """
                I've put together a draft on:

                "%s"

                It's in a Word document, ready for you to look over.%s

                Tell me what to change, or say yes when you're happy with it.""".formatted(
                request == null ? "(no confirmed request)" : request,
                revisionNote);

        Map<String, String> output = new LinkedHashMap<>();
        output.put(WorkflowPayload.DRAFT_DOCUMENT, "(not generated yet)");
        output.put(WorkflowPayload.RESEARCH_SUMMARY, "Draft prepared for: " + request);
        output.put("research.attempt", String.valueOf(context.attempt()));

        return StageResult.of(message, output);
    }
}
