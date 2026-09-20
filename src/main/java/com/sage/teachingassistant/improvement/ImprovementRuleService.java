package com.sage.teachingassistant.improvement;

import com.sage.teachingassistant.api.dto.FeedbackRequest;
import com.sage.teachingassistant.api.dto.ImprovementRuleResponse;
import com.sage.teachingassistant.domain.ImprovementRule;
import com.sage.teachingassistant.repository.ImprovementRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for the Improvement Layer: persists user feedback suggestions into the database
 * and synthesizes active rules dynamically into the LLM system prompt.
 */
@Service
public class ImprovementRuleService {

    private static final Logger log = LoggerFactory.getLogger(ImprovementRuleService.class);

    private final ImprovementRuleRepository ruleRepository;

    public ImprovementRuleService(ImprovementRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    /**
     * Records a feedback suggestion from the mobile app.
     */
    public ImprovementRuleResponse recordFeedback(FeedbackRequest request) {
        String text = request.resolveSuggestion();
        if (text.isBlank()) {
            throw new IllegalArgumentException("Feedback suggestion must not be blank");
        }

        String stageKey = request.resolveStageKey();
        ImprovementRule rule = new ImprovementRule(
                request.runId(),
                stageKey,
                request.workflowKey(),
                text,
                request.category());

        ImprovementRule saved = ruleRepository.save(rule);
        log.info("Recorded new improvement rule #{} for stage '{}': {}", saved.getId(), stageKey, text);
        return ImprovementRuleResponse.from(saved);
    }

    /**
     * Synthesizes active rules into a strict markdown prompt block for the LLM.
     * Checks both stage-specific rules and global ('ALL') rules.
     */
    public String buildRulePrompt(String stageKey) {
        List<String> targetStages = (stageKey != null && !stageKey.equalsIgnoreCase("ALL"))
                ? List.of(stageKey.trim(), "ALL")
                : List.of("ALL");

        List<ImprovementRule> activeRules = ruleRepository.findByStageKeyInAndIsActiveTrueOrderByCreatedAtAsc(targetStages);
        if (activeRules.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n### CONTINUOUS IMPROVEMENT RULES & USER PREFERENCES (MUST ADHERE TO):\n");
        sb.append("The teacher/user has specified the following persistent rules and guidelines for this step:\n");

        int index = 1;
        for (ImprovementRule rule : activeRules) {
            String clean = rule.getRuleText().trim();
            if (!clean.isEmpty()) {
                sb.append(index++).append(". ").append(clean).append("\n");
            }
        }

        sb.append("Strictly adhere to all of the above guidelines in your generated response.");
        return sb.toString();
    }

    /**
     * Returns all active improvement rules.
     */
    public List<ImprovementRuleResponse> listActiveRules() {
        return ruleRepository.findByIsActiveTrueOrderByCreatedAtDesc().stream()
                .map(ImprovementRuleResponse::from)
                .toList();
    }

    /**
     * Returns rules associated with a specific stage.
     */
    public List<ImprovementRuleResponse> listRulesForStage(String stageKey) {
        if (stageKey == null || stageKey.isBlank() || stageKey.equalsIgnoreCase("ALL")) {
            return listActiveRules();
        }
        return ruleRepository.findByStageKeyOrderByCreatedAtDesc(stageKey.trim()).stream()
                .map(ImprovementRuleResponse::from)
                .toList();
    }

    /**
     * Toggles rule activation status.
     */
    public boolean setRuleActive(Long id, boolean active) {
        return ruleRepository.findById(id).map(rule -> {
            rule.setActive(active);
            ruleRepository.save(rule);
            log.info("Set improvement rule #{} active status to {}", id, active);
            return true;
        }).orElse(false);
    }
}
