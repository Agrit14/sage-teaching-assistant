package com.sage.teachingassistant.api.dto;

import com.sage.teachingassistant.domain.Learner;

import java.time.Instant;

/** Public view of a learner. Never exposes the internal row id as the key. */
public record LearnerResponse(
        Long id,
        Long chatId,
        String username,
        String firstName,
        String lastName,
        String languageCode,
        Instant createdAt,
        Instant lastSeenAt
) {

    public static LearnerResponse from(Learner learner) {
        return new LearnerResponse(
                learner.getId(),
                learner.getTelegramChatId(),
                learner.getUsername(),
                learner.getFirstName(),
                learner.getLastName(),
                learner.getLanguageCode(),
                learner.getCreatedAt(),
                learner.getLastSeenAt());
    }
}
