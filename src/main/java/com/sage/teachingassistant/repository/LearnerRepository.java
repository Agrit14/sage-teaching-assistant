package com.sage.teachingassistant.repository;

import com.sage.teachingassistant.domain.Learner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LearnerRepository extends JpaRepository<Learner, Long> {

    Optional<Learner> findByTelegramChatId(Long telegramChatId);
}
