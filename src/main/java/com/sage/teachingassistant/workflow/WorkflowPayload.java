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

    /** Stage 1 output: research outline or blueprint confirmed by user. */
    public static final String CONFIRMED_OUTLINE = "research.confirmedOutline";

    /** Stage 2 output: generated textual content before or alongside docx. */
    public static final String GENERATED_CONTENT = "draft.generatedContent";

    /** Stage 2 output: where the generated Word document lives. */
    public static final String DRAFT_DOCUMENT = "research.draftDocument";

    /** Stage 2 output: file path to generated .docx on disk. */
    public static final String DOCX_FILE_PATH = "draft.docxFilePath";

    /** Stage 2 output: relative download URL for the docx file. */
    public static final String DOCX_DOWNLOAD_URL = "draft.docxDownloadUrl";

    /** Stage 2 output: short description of what the research covered. */
    public static final String RESEARCH_SUMMARY = "research.summary";

    /** Stage 3 output: where the generated PDF lives. */
    public static final String FINAL_PDF = "template.finalPdf";

    /** Stage 3 output: file path to final PDF on disk. */
    public static final String PDF_FILE_PATH = "template.pdfFilePath";

    /** Stage 3 output: relative download URL for the final PDF. */
    public static final String PDF_DOWNLOAD_URL = "template.pdfDownloadUrl";
}
