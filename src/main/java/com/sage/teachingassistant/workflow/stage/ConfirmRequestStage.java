package com.sage.teachingassistant.workflow.stage;

import com.sage.teachingassistant.workflow.StageContext;
import com.sage.teachingassistant.workflow.StageResult;
import com.sage.teachingassistant.workflow.WorkflowPayload;
import com.sage.teachingassistant.workflow.WorkflowStage;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stage 1 — establish what the user actually wants.
 *
 * <p>This is the only stage that can loop without anything having been produced
 * yet. If the user does not confirm, the engine re-runs this stage with their new
 * message, and it asks again. Nothing downstream runs until they say yes.
 *
 * <p><b>Tool not yet chosen.</b> The plan discussed was a web search here, so the
 * confirmation can be grounded in what actually exists on the topic rather than a
 * restatement of the user's own words.
 */
@Component
public class ConfirmRequestStage implements WorkflowStage {

    public static final String WORKFLOW_KEY = "educational-notes";
    public static final String STAGE_KEY = "confirm";

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
        return "Confirm the request";
    }

    @Override
    public StageResult execute(StageContext context) {
        String stated = context.latestUserInput();
        String understanding = stated == null ? "" : stated.strip();

        // TODO: stage tool. Look the topic up, then confirm against what was found
        //       rather than echoing the user's wording back at them.

        String message = """
                Before I build anything, let me check I've understood you.

                You're asking for:

                "%s"

                Is that right? Say yes and I'll start the research.""".formatted(understanding);

        Map<String, String> output = new LinkedHashMap<>();
        output.put(WorkflowPayload.CONFIRMED_REQUEST, understanding);
        output.put("confirm.attempt", String.valueOf(context.attempt()));

        return StageResult.of(message, output);
    }
}
