package com.sage.teachingassistant.service;

import com.sage.teachingassistant.domain.Learner;
import com.sage.teachingassistant.domain.MessageDirection;
import com.sage.teachingassistant.domain.MessageLog;
import com.sage.teachingassistant.repository.MessageLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MessageLogService {

    /** Telegram's own limit; anything longer is truncated before it hits the column. */
    private static final int MAX_STORED_LENGTH = 4096;

    private final MessageLogRepository messageLogRepository;

    public MessageLogService(MessageLogRepository messageLogRepository) {
        this.messageLogRepository = messageLogRepository;
    }

    public MessageLog record(Learner learner,
                             MessageDirection direction,
                             String text,
                             Long telegramMessageId) {
        return messageLogRepository.save(
                new MessageLog(learner, direction, truncate(text), telegramMessageId));
    }

    @Transactional(readOnly = true)
    public Page<MessageLog> history(Long learnerId, Pageable pageable) {
        return messageLogRepository.findByLearnerIdOrderByCreatedAtDesc(learnerId, pageable);
    }

    private String truncate(String text) {
        if (text == null || text.length() <= MAX_STORED_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_STORED_LENGTH);
    }
}
