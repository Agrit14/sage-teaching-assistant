package com.sage.teachingassistant.telegram;

import com.sage.teachingassistant.config.SageTelegramProperties;
import com.sage.teachingassistant.config.TelegramBotConfiguredCondition;
import com.sage.teachingassistant.service.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.DefaultLongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

/**
 * The bot itself.
 *
 * <p>Extends {@link DefaultLongPollingUpdateConsumer} so updates are handled one at
 * a time on a dedicated daemon thread. The Spring starter picks up every
 * {@link SpringLongPollingBot} bean and registers it automatically.
 */
@Component
@Conditional(TelegramBotConfiguredCondition.class)
public class SageTelegramBot extends DefaultLongPollingUpdateConsumer implements SpringLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(SageTelegramBot.class);

    private final SageTelegramProperties properties;
    private final ConversationService conversationService;

    public SageTelegramBot(SageTelegramProperties properties, ConversationService conversationService) {
        this.properties = properties;
        this.conversationService = conversationService;
    }

    @Override
    public String getBotToken() {
        return properties.token();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage()) {
            log.debug("Ignoring non-message update {}", update.getUpdateId());
            return;
        }

        Message message = update.getMessage();
        if (!message.hasText()) {
            // Photos, stickers, voice notes and so on are not handled yet.
            log.debug("Ignoring non-text message {}", message.getMessageId());
            return;
        }

        try {
            conversationService.handleIncoming(message);
        } catch (RuntimeException e) {
            // Never let one bad update kill the polling loop.
            log.error("Failed to handle update {}", update.getUpdateId(), e);
        }
    }

    @AfterBotRegistration
    public void onRegistered() {
        log.info("Sage is live and polling Telegram as @{}", properties.username());
    }
}
