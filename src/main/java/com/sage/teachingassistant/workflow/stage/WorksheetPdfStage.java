package com.sage.teachingassistant.workflow.stage;

import com.sage.teachingassistant.document.FileStorageService;
import com.sage.teachingassistant.document.PdfExportService;
import com.sage.teachingassistant.workflow.StageContext;
import com.sage.teachingassistant.workflow.StageResult;
import com.sage.teachingassistant.workflow.WorkflowPayload;
import com.sage.teachingassistant.workflow.WorkflowStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class WorksheetPdfStage implements WorkflowStage {

    private static final Logger log = LoggerFactory.getLogger(WorksheetPdfStage.class);
    public static final String WORKFLOW_KEY = "worksheet-generation";
    public static final String STAGE_KEY = "worksheet-pdf";

    private final PdfExportService pdfExportService;
    private final FileStorageService storageService;

    public WorksheetPdfStage(PdfExportService pdfExportService, FileStorageService storageService) {
        this.pdfExportService = pdfExportService;
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
        return "Export Final Alpha Tutor Worksheet PDF";
    }

    @Override
    public StageResult execute(StageContext context) {
        String request = context.priorOutput(WorkflowPayload.CONFIRMED_REQUEST);
        String content = context.priorOutput(WorkflowPayload.GENERATED_CONTENT);
        String downloadUrl = "/api/v1/workflows/runs/" + context.runId() + "/files/pdf";
        String pdfPathStr = "";

        try {
            Optional<File> docxFile = storageService.findDocx(context.runId());
            byte[] pdfBytes;

            if (docxFile.isPresent()) {
                byte[] docxBytes = Files.readAllBytes(docxFile.get().toPath());
                pdfBytes = pdfExportService.convertDocxToPdf(docxBytes);
            } else {
                pdfBytes = pdfExportService.exportPdf(
                        "CLASSROOM WORKSHEET",
                        request != null ? request : "Alpha Tutor Worksheet",
                        content != null ? content : "");
            }

            File savedPdf = storageService.savePdf(context.runId(), "worksheet", pdfBytes);
            pdfPathStr = savedPdf.getAbsolutePath();
        } catch (Exception e) {
            log.error("Failed to export PDF for run {}: {}", context.runId(), e.getMessage(), e);
        }

        String message = """
                Your Alpha Tutor Worksheet is ready!
                The approved Word draft has been converted into the branded PDF.

                📄 Final PDF File: Alpha_Tutor_Worksheet_%s.pdf
                📥 Download / View Link: %s

                Click **Confirm** (or reply "yes") to mark this run complete and save all files.""".formatted(
                context.runId().substring(0, 8),
                downloadUrl);

        Map<String, String> output = new LinkedHashMap<>();
        output.put(WorkflowPayload.FINAL_PDF, pdfPathStr);
        output.put(WorkflowPayload.PDF_FILE_PATH, pdfPathStr);
        output.put(WorkflowPayload.PDF_DOWNLOAD_URL, downloadUrl);
        output.put(WorkflowPayload.STAGE_CAN_CONFIRM, "true");
        output.put(WorkflowPayload.STAGE_ACTION, "complete_run");
        output.put("worksheet.pdf.attempt", String.valueOf(context.attempt()));

        return StageResult.of(message, output);
    }
}
