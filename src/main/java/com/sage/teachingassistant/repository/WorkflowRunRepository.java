package com.sage.teachingassistant.repository;

import com.sage.teachingassistant.domain.RunStatus;
import com.sage.teachingassistant.domain.WorkflowRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowRunRepository extends JpaRepository<WorkflowRun, String> {

    List<WorkflowRun> findByStatusOrderByCreatedAtDesc(RunStatus status);
}
