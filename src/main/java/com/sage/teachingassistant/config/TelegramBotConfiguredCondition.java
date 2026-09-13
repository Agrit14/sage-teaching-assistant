package com.sage.teachingassistant.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.NonNull;

/**
 * Matches only when a Telegram bot token is configured and the bot is not
 * switched off.
 *
 * <p>Without this, the Telegram starter would try to register a bot with a blank
 * token at startup and fail the whole application. Guarding on the token lets the
 * REST API boot and be tested before you ever visit BotFather.
 */
public class TelegramBotConfiguredCondition implements Condition {

    private static final String TOKEN_KEY = "sage.telegram.bot.token";
    private static final String ENABLED_KEY = "sage.telegram.bot.enabled";

    @Override
    public boolean matches(@NonNull ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
        String token = context.getEnvironment().getProperty(TOKEN_KEY, "");
        boolean enabled = Boolean.parseBoolean(
                context.getEnvironment().getProperty(ENABLED_KEY, "true"));

        return enabled && token != null && !token.isBlank();
    }
}
