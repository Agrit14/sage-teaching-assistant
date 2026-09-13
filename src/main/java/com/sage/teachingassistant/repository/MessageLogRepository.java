package com.sage.teachingassistant.repository;

import com.sage.teachingassistant.domain.MessageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageLogRepository extends JpaRepository<MessageLog, Long> {

    Page<MessageLog> findByLearnerIdOrderByCreatedAtDesc(Long learnerId, Pageable pageable);
}
