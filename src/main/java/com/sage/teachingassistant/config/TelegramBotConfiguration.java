package com.sage.teachingassistant.config;

import com.sage.teachingassistant.telegram.CommandRouter;
import com.sage.teachingassistant.telegram.TelegramMessenger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Wires up the pieces that only exist when a bot token is configured.
 *
 * <p>Keeping them behind the same condition as the bot means the application
 * starts cleanly with no token at all — handy for running the REST API and the
 * database on their own.
 */
@Configuration
@Conditional(TelegramBotConfiguredCondition.class)
public class TelegramBotConfiguration {

    @Bean
    public TelegramClient telegramClient(SageTelegramProperties properties) {
        return new OkHttpTelegramClient(properties.token());
    }

    @Bean
    public TelegramMessenger telegramMessenger(TelegramClient telegramClient) {
        return new TelegramMessenger(telegramClient);
    }

    @Bean
    public CommandRouter commandRouter(SageTelegramProperties properties) {
        return new CommandRouter(properties.username());
    }
}
