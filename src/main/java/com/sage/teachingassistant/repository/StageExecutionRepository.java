package com.sage.teachingassistant.repository;

import com.sage.teachingassistant.domain.ExecutionStatus;
import com.sage.teachingassistant.domain.StageExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StageExecutionRepository extends JpaRepository<StageExecution, Long> {

    List<StageExecution> findByRunIdOrderByCreatedAtAsc(String runId);

    Optional<StageExecution> findFirstByRunIdAndStageKeyOrderByAttemptDesc(String runId, String stageKey);

    long countByRunIdAndStageKey(String runId, String stageKey);

    List<StageExecution> findByRunIdAndStageIndexAndStatus(String runId,
                                                           Integer stageIndex,
                                                           ExecutionStatus status);
}
