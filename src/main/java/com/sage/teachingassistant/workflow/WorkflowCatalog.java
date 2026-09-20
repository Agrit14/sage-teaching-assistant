package com.sage.teachingassistant.workflow;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Declares the workflows the engine knows about.
 *
 * <p>This is the one place that says what runs in what order. Stages themselves
 * carry no ordering, so the sequence cannot drift out of sync with the code.
 */
@Configuration
public class WorkflowCatalog {

    /** Original educational notes workflow (kept for backward compatibility). */
    @Bean
    public WorkflowDefinition educationalNotesWorkflow(ConfirmRequestStage confirmRequest,
                                                       ResearchStage research,
                                                       TemplateStage template) {
        return new WorkflowDefinition(
                ConfirmRequestStage.WORKFLOW_KEY,
                "Educational notes and worksheet",
                List.of(confirmRequest, research, template));
    }

    /** 1. Alpha Tutor Worksheet Generation Workflow */
    @Bean
    public WorkflowDefinition worksheetGenerationWorkflow(WorksheetResearchStage research,
                                                          WorksheetDraftStage draft,
                                                          WorksheetPdfStage pdf) {
        return new WorkflowDefinition(
                WorksheetResearchStage.WORKFLOW_KEY,
                "Worksheet Generator (Alpha Tutor)",
                List.of(research, draft, pdf));
    }

    /** 2. Alpha Tutor Test Paper Generation Workflow */
    @Bean
    public WorkflowDefinition testGenerationWorkflow(TestResearchStage research,
                                                     TestDraftStage draft,
                                                     TestPdfStage pdf) {
        return new WorkflowDefinition(
                TestResearchStage.WORKFLOW_KEY,
                "Test Paper Generator (Alpha Tutor)",
                List.of(research, draft, pdf));
    }

    /** 3. Alpha Tutor Revision Notes Generation Workflow */
    @Bean
    public WorkflowDefinition notesGenerationWorkflow(NotesResearchStage research,
                                                      NotesDraftStage draft,
                                                      NotesPdfStage pdf) {
        return new WorkflowDefinition(
                NotesResearchStage.WORKFLOW_KEY,
                "Revision Notes Generator (Alpha Tutor)",
                List.of(research, draft, pdf));
    }

    /** 4. Direct PDF Print Workflow (DOCX upload -> Template PDF) */
    @Bean
    public WorkflowDefinition pdfPrintWorkflow(DirectPdfPrintStage printStage) {
        return new WorkflowDefinition(
                DirectPdfPrintStage.WORKFLOW_KEY,
                "Direct PDF Print with Alpha Tutor Template",
                List.of(printStage));
    }

    /** 5. General Educational Q&A Workflow (Direct LLM answering) */
    @Bean
    public WorkflowDefinition generalQaWorkflow(com.sage.teachingassistant.workflow.stage.GeneralQaStage qaStage) {
        return new WorkflowDefinition(
                com.sage.teachingassistant.workflow.stage.GeneralQaStage.WORKFLOW_KEY,
                "General Educational Q&A (Direct LLM)",
                List.of(qaStage));
    }
}
