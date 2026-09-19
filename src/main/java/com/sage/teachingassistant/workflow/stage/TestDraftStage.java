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
public class TestDraftStage implements WorkflowStage {

    private static final Logger log = LoggerFactory.getLogger(TestDraftStage.class);
    public static final String WORKFLOW_KEY = "test-generation";
    public static final String STAGE_KEY = "test-draft";

    private final EducationalContentService educationalService;
    private final DocxTemplateGenerator docxGenerator;
    private final FileStorageService storageService;

    public TestDraftStage(EducationalContentService educationalService,
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
        return "Generate Alpha Tutor Test Paper Word Document";
    }

    @Override
    public StageResult execute(StageContext context) {
        String request = context.priorOutput(WorkflowPayload.CONFIRMED_REQUEST);
        String blueprint = context.priorOutput(WorkflowPayload.CONFIRMED_OUTLINE);

        String testContent = educationalService.generateTestContent(
                request != null ? request : "Examination Paper",
                blueprint != null ? blueprint : "",
                context.feedback());

        String docxPathStr = "";
        String downloadUrl = "/api/v1/workflows/runs/" + context.runId() + "/files/docx";

        try {
            byte[] docxBytes = docxGenerator.generateDocument(
                    "EXAMINATION TEST PAPER",
                    request != null ? request : "Alpha Tutor Test Paper",
                    testContent);

            File saved = storageService.saveDocx(context.runId(), "test_paper", docxBytes);
            docxPathStr = saved.getAbsolutePath();
        } catch (Exception e) {
            log.error("Failed to generate DOCX for test run {}: {}", context.runId(), e.getMessage(), e);
        }

        String revisionNote = context.isRevision()
                ? "\n\n(Revisions applied based on your feedback: \"" + context.feedback().strip() + "\")\n"
                : "\n";

        String message = """
                The Alpha Tutor Test Paper Word document has been generated!%s
                Document: Alpha_Tutor_Test_%s.docx
                Download link: %s

                --- Summary of Examination Paper ---
                %s
                -------------------------------------

                Please review the questions, marks distribution, and answer key:
                • Say "yes" if satisfied, and I will export the print-ready PDF.
                • Or tell me your edits (e.g. "replace question 3 with a numerical").""".formatted(
                revisionNote,
                context.runId().substring(0, 8),
                downloadUrl,
                truncate(testContent, 600));

        Map<String, String> output = new LinkedHashMap<>();
        output.put(WorkflowPayload.CONFIRMED_REQUEST, request != null ? request : "");
        output.put(WorkflowPayload.GENERATED_CONTENT, testContent);
        output.put(WorkflowPayload.DRAFT_DOCUMENT, docxPathStr);
        output.put(WorkflowPayload.DOCX_FILE_PATH, docxPathStr);
        output.put(WorkflowPayload.DOCX_DOWNLOAD_URL, downloadUrl);
        output.put("test.draft.attempt", String.valueOf(context.attempt()));

        return StageResult.of(message, output);
    }

    private static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text == null ? "" : text;
        }
        return text.substring(0, maxLength) + "\n... [Full test paper and marking scheme in Word document]";
    }
}
