package com.sage.teachingassistant.workflow;

import com.sage.teachingassistant.ai.EducationalContentService;
import com.sage.teachingassistant.ai.GeminiClient;
import com.sage.teachingassistant.document.DocxTemplateGenerator;
import com.sage.teachingassistant.document.FileStorageService;
import com.sage.teachingassistant.document.PdfExportService;
import com.sage.teachingassistant.workflow.stage.ConfirmRequestStage;
import com.sage.teachingassistant.workflow.stage.DirectPdfPrintStage;
import com.sage.teachingassistant.workflow.stage.NotesDraftStage;
import com.sage.teachingassistant.workflow.stage.NotesPdfStage;
import com.sage.teachingassistant.workflow.stage.NotesResearchStage;
import com.sage.teachingassistant.workflow.stage.ResearchStage;
import com.sage.teachingassistant.workflow.stage.TemplateStage;
import com.sage.teachingassistant.workflow.stage.TestDraftStage;
import com.sage.teachingassistant.workflow.stage.TestPdfStage;
import com.sage.teachingassistant.workflow.stage.TestResearchStage;
import com.sage.teachingassistant.workflow.stage.WorksheetDraftStage;
import com.sage.teachingassistant.workflow.stage.WorksheetPdfStage;
import com.sage.teachingassistant.workflow.stage.WorksheetResearchStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowsCatalogTest {

    private WorkflowCatalog catalog;
    private EducationalContentService eduService;
    private DocxTemplateGenerator docxGen;
    private PdfExportService pdfService;
    private FileStorageService storageService;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        catalog = new WorkflowCatalog();
        GeminiClient geminiClient = new GeminiClient("", "", "", new com.fasterxml.jackson.databind.ObjectMapper());
        eduService = new EducationalContentService(geminiClient);
        docxGen = new DocxTemplateGenerator();
        pdfService = new PdfExportService();
        storageService = new FileStorageService(tempDir.toString());
    }

    @Test
    void testWorksheetWorkflowStages() {
        WorksheetResearchStage research = new WorksheetResearchStage(eduService);
        WorksheetDraftStage draft = new WorksheetDraftStage(eduService, docxGen, storageService);
        WorksheetPdfStage pdf = new WorksheetPdfStage(pdfService, storageService);

        WorkflowDefinition def = catalog.worksheetGenerationWorkflow(research, draft, pdf);
        assertThat(def.key()).isEqualTo("worksheet-generation");
        assertThat(def.stageCount()).isEqualTo(3);

        // Stage 0: Research
        StageContext ctx0 = new StageContext("run-ws-1", def.key(), "Class 10 Physics Electricity", null, 1, Map.of());
        StageResult res0 = research.execute(ctx0);
        assertThat(res0.message()).contains("Worksheet Outline");
        assertThat(res0.output()).containsKey(WorkflowPayload.CONFIRMED_OUTLINE);

        // Stage 1: Draft
        StageContext ctx1 = new StageContext("run-ws-1", def.key(), null, null, 1, res0.output());
        StageResult res1 = draft.execute(ctx1);
        assertThat(res1.message()).contains("Alpha Tutor Worksheet Word document");
        assertThat(res1.output()).containsKey(WorkflowPayload.DOCX_FILE_PATH);

        // Stage 2: PDF Export
        StageContext ctx2 = new StageContext("run-ws-1", def.key(), null, null, 1, res1.output());
        StageResult res2 = pdf.execute(ctx2);
        assertThat(res2.message()).contains("Alpha Tutor Worksheet is ready");
        assertThat(res2.output()).containsKey(WorkflowPayload.PDF_FILE_PATH);
    }

    @Test
    void testTestWorkflowStages() {
        TestResearchStage research = new TestResearchStage(eduService);
        TestDraftStage draft = new TestDraftStage(eduService, docxGen, storageService);
        TestPdfStage pdf = new TestPdfStage(pdfService, storageService);

        WorkflowDefinition def = catalog.testGenerationWorkflow(research, draft, pdf);
        assertThat(def.key()).isEqualTo("test-generation");
        assertThat(def.stageCount()).isEqualTo(3);

        StageContext ctx0 = new StageContext("run-test-1", def.key(), "Class 8 Math Linear Equations", null, 1, Map.of());
        StageResult res0 = research.execute(ctx0);
        assertThat(res0.message()).contains("Blueprint");

        StageContext ctx1 = new StageContext("run-test-1", def.key(), null, null, 1, res0.output());
        StageResult res1 = draft.execute(ctx1);
        assertThat(res1.message()).contains("Alpha Tutor Test Paper Word document");

        StageContext ctx2 = new StageContext("run-test-1", def.key(), null, null, 1, res1.output());
        StageResult res2 = pdf.execute(ctx2);
        assertThat(res2.message()).contains("Alpha Tutor Test Examination Paper is ready");
    }

    @Test
    void testNotesWorkflowStages() {
        NotesResearchStage research = new NotesResearchStage(eduService);
        NotesDraftStage draft = new NotesDraftStage(eduService, docxGen, storageService);
        NotesPdfStage pdf = new NotesPdfStage(pdfService, storageService);

        WorkflowDefinition def = catalog.notesGenerationWorkflow(research, draft, pdf);
        assertThat(def.key()).isEqualTo("notes-generation");
        assertThat(def.stageCount()).isEqualTo(3);

        StageContext ctx0 = new StageContext("run-notes-1", def.key(), "Class 9 Biology Cell Structure", null, 1, Map.of());
        StageResult res0 = research.execute(ctx0);
        assertThat(res0.message()).contains("Revision Notes Outline");

        StageContext ctx1 = new StageContext("run-notes-1", def.key(), null, null, 1, res0.output());
        StageResult res1 = draft.execute(ctx1);
        assertThat(res1.message()).contains("Alpha Tutor Revision Notes Word document");

        StageContext ctx2 = new StageContext("run-notes-1", def.key(), null, null, 1, res1.output());
        StageResult res2 = pdf.execute(ctx2);
        assertThat(res2.message()).contains("Alpha Tutor Revision Notes PDF is ready");
    }

    @Test
    void testDirectPdfPrintStage() {
        DirectPdfPrintStage printStage = new DirectPdfPrintStage(pdfService, storageService);
        WorkflowDefinition def = catalog.pdfPrintWorkflow(printStage);
        assertThat(def.key()).isEqualTo("pdf-print");
        assertThat(def.stageCount()).isEqualTo(1);

        // When no file uploaded yet
        StageContext ctx0 = new StageContext("run-print-1", def.key(), "uploading", null, 1, Map.of());
        StageResult res0 = printStage.execute(ctx0);
        assertThat(res0.message()).contains("Please upload your Word document");
    }
}
