package com.sage.teachingassistant.repository;

import com.sage.teachingassistant.domain.ImprovementRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImprovementRuleRepository extends JpaRepository<ImprovementRule, Long> {

    List<ImprovementRule> findByStageKeyInAndIsActiveTrueOrderByCreatedAtAsc(List<String> stageKeys);

    List<ImprovementRule> findByIsActiveTrueOrderByCreatedAtDesc();

    List<ImprovementRule> findByStageKeyOrderByCreatedAtDesc(String stageKey);

    List<ImprovementRule> findByRunIdOrderByCreatedAtAsc(String runId);
}
