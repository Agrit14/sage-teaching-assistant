package com.sage.teachingassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Telegram bot settings, bound from the {@code sage.telegram.bot.*} keys in
 * {@code application.yml}.
 *
 * @param token    the bot token issued by BotFather; blank means "no bot"
 * @param username the bot's @handle, used to strip {@code /command@handle} suffixes
 * @param enabled  master switch, lets you run the REST API without polling Telegram
 */
@ConfigurationProperties(prefix = "sage.telegram.bot")
public record SageTelegramProperties(String token, String username, boolean enabled) {

    public boolean hasToken() {
        return token != null && !token.isBlank();
    }

    /** True when the bot should actually start polling Telegram. */
    public boolean isActive() {
        return enabled && hasToken();
    }
}
