package com.sage.teachingassistant.workflow.stage;

import com.sage.teachingassistant.workflow.StageContext;
import com.sage.teachingassistant.workflow.StageResult;
import com.sage.teachingassistant.workflow.WorkflowPayload;
import com.sage.teachingassistant.workflow.WorkflowStage;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stage 3 — pour the approved draft into the template and produce a PDF.
 *
 * <p>The final stage. Approving it completes the run.
 *
 * <p><b>Tool not yet chosen.</b> The plan discussed was a Python script that takes
 * the Word document from stage 2 and renders it through the PDF template.
 */
@Component
public class TemplateStage implements WorkflowStage {

    public static final String WORKFLOW_KEY = "educational-notes";
    public static final String STAGE_KEY = "template";

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
        return "Apply template and export PDF";
    }

    @Override
    public StageResult execute(StageContext context) {
        String draft = context.priorOutput(WorkflowPayload.DRAFT_DOCUMENT);

        // TODO: stage tool. Run the approved draft through the PDF template and
        //       return the path to the finished file.

        String message = """
                The approved draft has been laid into the template.

                Source document: %s

                Final PDF: (not generated yet)

                Say yes to finish.""".formatted(draft == null ? "(missing)" : draft);

        Map<String, String> output = new LinkedHashMap<>();
        output.put(WorkflowPayload.FINAL_PDF, "(not generated yet)");
        output.put("template.attempt", String.valueOf(context.attempt()));

        return StageResult.of(message, output);
    }
}
