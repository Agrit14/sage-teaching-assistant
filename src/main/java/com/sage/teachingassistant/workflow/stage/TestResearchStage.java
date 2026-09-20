package com.sage.teachingassistant.workflow.stage;

import com.sage.teachingassistant.ai.EducationalContentService;
import com.sage.teachingassistant.workflow.StageContext;
import com.sage.teachingassistant.workflow.StageResult;
import com.sage.teachingassistant.workflow.TopicContext;
import com.sage.teachingassistant.workflow.WorkflowPayload;
import com.sage.teachingassistant.workflow.WorkflowStage;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TestResearchStage implements WorkflowStage {

    public static final String WORKFLOW_KEY = "test-generation";
    public static final String STAGE_KEY = "test-research";

    private final EducationalContentService educationalService;

    public TestResearchStage(EducationalContentService educationalService) {
        this.educationalService = educationalService;
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
        return "Test Paper Blueprint & Syllabus Research";
    }

    @Override
    public StageResult execute(StageContext context) {
        TopicContext topic = TopicContext.resolve(context);

        String blueprint = educationalService.researchTestBlueprint(
                topic.className(),
                topic.chapterName(),
                topic.additionalDetails(),
                context.feedback());

        Map<String, String> output = new LinkedHashMap<>();
        output.put(WorkflowPayload.TOPIC_CLASS, topic.className());
        output.put(WorkflowPayload.TOPIC_CHAPTER, topic.chapterName());
        output.put(WorkflowPayload.TOPIC_DETAILS, topic.additionalDetails());
        output.put(WorkflowPayload.CONFIRMED_REQUEST, topic.formattedTitle());
        output.put(WorkflowPayload.CONFIRMED_OUTLINE, blueprint);
        output.put(WorkflowPayload.STAGE_CAN_CONFIRM, "true");
        output.put(WorkflowPayload.STAGE_ACTION, "confirm_blueprint");
        output.put("test.attempt", String.valueOf(context.attempt()));

        return StageResult.of(blueprint, output);
    }
}
