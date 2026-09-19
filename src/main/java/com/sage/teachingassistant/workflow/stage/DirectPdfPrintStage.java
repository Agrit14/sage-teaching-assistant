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
public class DirectPdfPrintStage implements WorkflowStage {

    private static final Logger log = LoggerFactory.getLogger(DirectPdfPrintStage.class);
    public static final String WORKFLOW_KEY = "pdf-print";
    public static final String STAGE_KEY = "pdf-print-convert";

    private final PdfExportService pdfExportService;
    private final FileStorageService storageService;

    public DirectPdfPrintStage(PdfExportService pdfExportService, FileStorageService storageService) {
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
        return "Apply Alpha Tutor Template and Export PDF";
    }

    @Override
    public StageResult execute(StageContext context) {
        Optional<File> docxFile = storageService.findDocx(context.runId());
        String downloadUrl = "/api/v1/workflows/runs/" + context.runId() + "/files/pdf";
        String uploadUrl = "/api/v1/workflows/runs/" + context.runId() + "/upload";

        Map<String, String> output = new LinkedHashMap<>();

        if (docxFile.isEmpty()) {
            String message = """
                    Welcome to PDF Print.
                    Please upload your Word document (.docx) to format it into the Alpha Tutor template.

                    Upload Endpoint: POST %s (multipart/form-data with part 'file')
                    Once uploaded, send 'yes' to generate your PDF.""".formatted(uploadUrl);

            output.put("pdf_print.status", "AWAITING_UPLOAD");
            output.put("pdf_print.uploadUrl", uploadUrl);
            return StageResult.of(message, output);
        }

        String pdfPathStr = "";
        try {
            byte[] docxBytes = Files.readAllBytes(docxFile.get().toPath());
            byte[] pdfBytes = pdfExportService.convertDocxToPdf(docxBytes);
            File saved = storageService.savePdf(context.runId(), "print", pdfBytes);
            pdfPathStr = saved.getAbsolutePath();
        } catch (Exception e) {
            log.error("Failed to convert uploaded docx to PDF for run {}: {}", context.runId(), e.getMessage(), e);
            String errorMsg = "Conversion failed: " + e.getMessage() + ". Please check your Word document and re-upload.";
            output.put("pdf_print.error", e.getMessage());
            return StageResult.of(errorMsg, output);
        }

        String message = """
                Your Word document has been formatted into the Alpha Tutor PDF template!

                Original File: %s
                Final PDF File: Alpha_Tutor_Print_%s.pdf
                Download Link: %s

                Say "yes" to complete the print job.""".formatted(
                docxFile.get().getName(),
                context.runId().substring(0, 8),
                downloadUrl);

        output.put(WorkflowPayload.FINAL_PDF, pdfPathStr);
        output.put(WorkflowPayload.PDF_FILE_PATH, pdfPathStr);
        output.put(WorkflowPayload.PDF_DOWNLOAD_URL, downloadUrl);
        output.put("pdf_print.attempt", String.valueOf(context.attempt()));

        return StageResult.of(message, output);
    }
}
