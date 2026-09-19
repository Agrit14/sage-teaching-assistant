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
public class NotesDraftStage implements WorkflowStage {

    private static final Logger log = LoggerFactory.getLogger(NotesDraftStage.class);
    public static final String WORKFLOW_KEY = "notes-generation";
    public static final String STAGE_KEY = "notes-draft";

    private final EducationalContentService educationalService;
    private final DocxTemplateGenerator docxGenerator;
    private final FileStorageService storageService;

    public NotesDraftStage(EducationalContentService educationalService,
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
        return "Generate Alpha Tutor Revision Notes Word Document";
    }

    @Override
    public StageResult execute(StageContext context) {
        String request = context.priorOutput(WorkflowPayload.CONFIRMED_REQUEST);
        String outline = context.priorOutput(WorkflowPayload.CONFIRMED_OUTLINE);

        String notesContent = educationalService.generateNotesContent(
                request != null ? request : "Revision Notes",
                outline != null ? outline : "",
                context.feedback());

        String docxPathStr = "";
        String downloadUrl = "/api/v1/workflows/runs/" + context.runId() + "/files/docx";

        try {
            byte[] docxBytes = docxGenerator.generateDocument(
                    "REVISION STUDY NOTES",
                    request != null ? request : "Alpha Tutor Revision Notes",
                    notesContent);

            File saved = storageService.saveDocx(context.runId(), "revision_notes", docxBytes);
            docxPathStr = saved.getAbsolutePath();
        } catch (Exception e) {
            log.error("Failed to generate notes DOCX for run {}: {}", context.runId(), e.getMessage(), e);
        }

        String revisionNote = context.isRevision()
                ? "\n\n(Revisions applied based on your feedback: \"" + context.feedback().strip() + "\")\n"
                : "\n";

        String message = """
                The Alpha Tutor Revision Notes Word document has been generated!%s
                Document: Alpha_Tutor_Notes_%s.docx
                Download link: %s

                --- Summary of Revision Notes ---
                %s
                ----------------------------------

                Please review the notes:
                • Say "yes" to export the final PDF with Alpha Tutor branding.
                • Or tell me what points to add, simplify, or rephrase.""".formatted(
                revisionNote,
                context.runId().substring(0, 8),
                downloadUrl,
                truncate(notesContent, 600));

        Map<String, String> output = new LinkedHashMap<>();
        output.put(WorkflowPayload.CONFIRMED_REQUEST, request != null ? request : "");
        output.put(WorkflowPayload.GENERATED_CONTENT, notesContent);
        output.put(WorkflowPayload.DRAFT_DOCUMENT, docxPathStr);
        output.put(WorkflowPayload.DOCX_FILE_PATH, docxPathStr);
        output.put(WorkflowPayload.DOCX_DOWNLOAD_URL, downloadUrl);
        output.put("notes.draft.attempt", String.valueOf(context.attempt()));

        return StageResult.of(message, output);
    }

    private static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text == null ? "" : text;
        }
        return text.substring(0, maxLength) + "\n... [Full revision notes available in Word document]";
    }
}
