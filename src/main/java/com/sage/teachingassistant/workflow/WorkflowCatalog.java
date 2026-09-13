package com.sage.teachingassistant.workflow;

import com.sage.teachingassistant.workflow.stage.ConfirmRequestStage;
import com.sage.teachingassistant.workflow.stage.ResearchStage;
import com.sage.teachingassistant.workflow.stage.TemplateStage;
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

    @Bean
    public WorkflowDefinition educationalNotesWorkflow(ConfirmRequestStage confirmRequest,
                                                       ResearchStage research,
                                                       TemplateStage template) {
        return new WorkflowDefinition(
                ConfirmRequestStage.WORKFLOW_KEY,
                "Educational notes and worksheet",
                List.of(confirmRequest, research, template));
    }
}
