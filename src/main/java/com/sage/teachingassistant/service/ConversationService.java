package com.sage.teachingassistant.service;

import com.sage.teachingassistant.config.TelegramBotConfiguredCondition;
import com.sage.teachingassistant.domain.Learner;
import com.sage.teachingassistant.domain.MessageDirection;
import com.sage.teachingassistant.telegram.CommandRouter;
import com.sage.teachingassistant.telegram.TelegramMessenger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

/**
 * Orchestrates one turn of conversation: persist who said what, work out the
 * reply, send it, persist that too.
 *
 * <p>Only active when a bot token is configured. It deliberately does not hold a
 * database transaction open across the Telegram call — the two writes go through
 * {@link LearnerService} and {@link MessageLogService}, which each manage their own.
 */
@Service
@Conditional(TelegramBotConfiguredCondition.class)
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    /** How much of an unrecognised message to quote back. */
    private static final int QUOTE_LIMIT = 160;

    private final LearnerService learnerService;
    private final MessageLogService messageLogService;
    private final TelegramMessenger messenger;
    private final CommandRouter commandRouter;

    public ConversationService(LearnerService learnerService,
                               MessageLogService messageLogService,
                               TelegramMessenger messenger,
                               CommandRouter commandRouter) {
        this.learnerService = learnerService;
        this.messageLogService = messageLogService;
        this.messenger = messenger;
        this.commandRouter = commandRouter;
    }

    /** Handles one inbound text message and replies to it. */
    public void handleIncoming(Message message) {
        Long chatId = message.getChatId();
        if (chatId == null) {
            log.warn("Ignoring message {} with no chat id", message.getMessageId());
            return;
        }

        User from = message.getFrom();
        Learner learner = learnerService.findOrCreate(
                chatId,
                from == null ? null : from.getId(),
                from == null ? null : from.getUserName(),
                from == null ? null : from.getFirstName(),
                from == null ? null : from.getLastName(),
                from == null ? null : from.getLanguageCode());

        String text = message.getText();
        Long telegramMessageId = message.getMessageId() == null
                ? null
                : message.getMessageId().longValue();

        messageLogService.record(learner, MessageDirection.INBOUND, text, telegramMessageId);

        String reply = commandRouter.replyFor(text, chatId, learner.displayName())
                .orElseGet(() -> notYetTeaching(text));

        messenger.sendText(chatId, reply);
        messageLogService.record(learner, MessageDirection.OUTBOUND, reply, null);
    }

    /**
     * Placeholder for free-form messages. Honest about the current state rather
     * than pretending to teach — the tutoring behaviour lands in a later pass.
     */
    private String notYetTeaching(String text) {
        return """
                You said: "%s"

                I've saved that. My teaching side isn't switched on yet — for now \
                I only understand commands. Send /help to see them, or /about to \
                find out what I'm meant to become.""".formatted(quote(text));
    }

    private String quote(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.strip();
        return trimmed.length() <= QUOTE_LIMIT
                ? trimmed
                : trimmed.substring(0, QUOTE_LIMIT) + "...";
    }
}
