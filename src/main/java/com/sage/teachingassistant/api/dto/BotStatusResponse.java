package com.sage.teachingassistant.api.dto;

/** Health of the Telegram side of the application. */
public record BotStatusResponse(
        boolean botActive,
        String botUsername,
        long learnerCount
) {
}
