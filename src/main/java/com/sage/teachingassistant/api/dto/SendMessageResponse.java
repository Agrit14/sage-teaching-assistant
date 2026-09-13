package com.sage.teachingassistant.api.dto;

/** Result of an outbound send. */
public record SendMessageResponse(
        long chatId,
        Integer telegramMessageId,
        boolean delivered,
        boolean logged
) {
}
