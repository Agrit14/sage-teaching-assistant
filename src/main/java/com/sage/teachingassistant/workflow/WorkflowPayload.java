package com.sage.teachingassistant.workflow;

/**
 * The agreed keys stages use to hand data forward.
 *
 * <p>Stages never reference each other's classes. They reference these names, so a
 * stage can be rewritten or replaced without touching its neighbours — as long as
 * it honours this contract.
 */
public final class WorkflowPayload {

    private WorkflowPayload() {
    }

    /** Stage 1 output: the request as Sage understood it, ready for research. */
    public static final String CONFIRMED_REQUEST = "confirm.confirmedRequest";

    /** Stage 2 output: where the generated Word document lives. */
    public static final String DRAFT_DOCUMENT = "research.draftDocument";

    /** Stage 2 output: short description of what the research covered. */
    public static final String RESEARCH_SUMMARY = "research.summary";

    /** Stage 3 output: where the generated PDF lives. */
    public static final String FINAL_PDF = "template.finalPdf";
}
