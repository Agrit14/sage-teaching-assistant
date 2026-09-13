package com.sage.teachingassistant.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Thin wrapper around the Telegram client.
 *
 * <p>Two things it takes care of so callers do not have to:
 * <ul>
 *   <li>Telegram rejects any message longer than 4096 characters, so long replies
 *       are split on word boundaries.</li>
 *   <li>A failed send is logged, not thrown. One undeliverable message should never
 *       take down the polling loop.</li>
 * </ul>
 */
public class TelegramMessenger {

    private static final Logger log = LoggerFactory.getLogger(TelegramMessenger.class);

    /** Telegram's hard limit on a single text message. */
    static final int MAX_MESSAGE_LENGTH = 4096;

    private final TelegramClient telegramClient;

    public TelegramMessenger(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    /** Builds a messenger backed by a fresh OkHttp client for the given bot token. */
    public static TelegramMessenger forToken(String botToken) {
        return new TelegramMessenger(new OkHttpTelegramClient(botToken));
    }

    /**
     * Sends {@code text} to {@code chatId}, splitting it if needed.
     *
     * @return the id of the first message actually delivered, or {@code null} if nothing went out
     */
    public Integer sendText(long chatId, String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Integer firstMessageId = null;
        for (String chunk : split(text)) {
            try {
                Message sent = telegramClient.execute(
                        SendMessage.builder()
                                .chatId(chatId)
                                .text(chunk)
                                .build());
                if (firstMessageId == null && sent != null) {
                    firstMessageId = sent.getMessageId();
                }
            } catch (TelegramApiException e) {
                log.error("Failed to send message to chat {}: {}", chatId, e.getMessage());
            }
        }
        return firstMessageId;
    }

    /**
     * Splits text into Telegram-sized chunks, preferring to break on a newline or
     * space near the limit rather than mid-word.
     */
    static List<String> split(String text) {
        if (text.length() <= MAX_MESSAGE_LENGTH) {
            return List.of(text);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + MAX_MESSAGE_LENGTH, text.length());
            if (end < text.length()) {
                int breakAt = text.lastIndexOf('\n', end);
                if (breakAt <= start) {
                    breakAt = text.lastIndexOf(' ', end);
                }
                if (breakAt > start) {
                    end = breakAt;
                }
            }
            chunks.add(text.substring(start, end).strip());
            start = end;
        }
        return chunks;
    }
}
