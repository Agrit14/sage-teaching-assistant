package com.sage.teachingassistant.workflow.stage;

import com.sage.teachingassistant.ai.EducationalContentService;
import com.sage.teachingassistant.document.DocxTemplateGenerator;
import com.sage.teachingassistant.document.FileStorageService;
import com.sage.teachingassistant.workflow.StageContext;
import com.sage.teachingassistant.workflow.StageResult;
import com.sage.teachingassistant.workflow.WorkflowPayload;
import com.sage.teachingassistant.workflow.WorkflowStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class WorksheetDraftStage implements WorkflowStage {

    private static final Logger log = LoggerFactory.getLogger(WorksheetDraftStage.class);
    public static final String WORKFLOW_KEY = "worksheet-generation";
    public static final String STAGE_KEY = "worksheet-draft";

    private final EducationalContentService educationalService;
    private final DocxTemplateGenerator docxGenerator;
    private final FileStorageService storageService;

    public WorksheetDraftStage(EducationalContentService educationalService,
                               DocxTemplateGenerator docxGenerator,
                               FileStorageService storageService) {
        this.educationalService = educationalService;
        this.docxGenerator = docxGenerator;
        this.storageService = storageService;
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
        return "Generate Alpha Tutor Worksheet Word Document";
    }

    @Override
    public StageResult execute(StageContext context) {
        String request = context.priorOutput(WorkflowPayload.CONFIRMED_REQUEST);
        String outline = context.priorOutput(WorkflowPayload.CONFIRMED_OUTLINE);

        String worksheetContent = educationalService.generateWorksheetContent(
                request != null ? request : "Worksheet",
                outline != null ? outline : "",
                context.feedback());

        String docxPathStr = "";
        String downloadUrl = "/api/v1/workflows/runs/" + context.runId() + "/files/docx";

        try {
            byte[] docxBytes = docxGenerator.generateDocument(
                    "CLASSROOM WORKSHEET",
                    request != null ? request : "Alpha Tutor Worksheet",
                    worksheetContent);

            File saved = storageService.saveDocx(context.runId(), "worksheet", docxBytes);
            docxPathStr = saved.getAbsolutePath();
        } catch (Exception e) {
            log.error("Failed to generate DOCX for run {}: {}", context.runId(), e.getMessage(), e);
        }

        String revisionNote = context.isRevision()
                ? "\n\n(Revisions applied based on your feedback: \"" + context.feedback().strip() + "\")\n"
                : "\n";

        String message = """
                The Alpha Tutor Worksheet Word document has been generated!%s
                📄 Word Document: Alpha_Tutor_Worksheet_%s.docx
                📥 Download link: %s

                --- Summary of Worksheet Content ---
                %s
                -------------------------------------

                Please review the Word document:
                • Click **Confirm** (or reply "yes") if it looks good, and I will export the final branded Alpha Tutor PDF.
                • Or reply with any changes, questions, or adjustments you'd like.""".formatted(
                revisionNote,
                context.runId().substring(0, 8),
                downloadUrl,
                truncate(worksheetContent, 600));

        Map<String, String> output = new LinkedHashMap<>();
        output.put(WorkflowPayload.CONFIRMED_REQUEST, request != null ? request : "");
        output.put(WorkflowPayload.GENERATED_CONTENT, worksheetContent);
        output.put(WorkflowPayload.DRAFT_DOCUMENT, docxPathStr);
        output.put(WorkflowPayload.DOCX_FILE_PATH, docxPathStr);
        output.put(WorkflowPayload.DOCX_DOWNLOAD_URL, downloadUrl);
        output.put(WorkflowPayload.STAGE_CAN_CONFIRM, "true");
        output.put(WorkflowPayload.STAGE_ACTION, "confirm_docx");
        output.put("worksheet.draft.attempt", String.valueOf(context.attempt()));

        return StageResult.of(message, output);
    }

    private static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text == null ? "" : text;
        }
        return text.substring(0, maxLength) + "\n... [Full content available in Word document]";
    }
}
