package com.sage.teachingassistant.api.dto;

import com.sage.teachingassistant.domain.MessageLog;

import java.time.Instant;

/** Public view of one logged message. */
public record MessageResponse(
        Long id,
        String direction,
        String text,
        Long telegramMessageId,
        Instant createdAt
) {

    public static MessageResponse from(MessageLog log) {
        return new MessageResponse(
                log.getId(),
                log.getDirection().name(),
                log.getText(),
                log.getTelegramMessageId(),
                log.getCreatedAt());
    }
}
