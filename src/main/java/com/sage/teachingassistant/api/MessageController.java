package com.sage.teachingassistant.api;

import com.sage.teachingassistant.api.dto.SendMessageRequest;
import com.sage.teachingassistant.api.dto.SendMessageResponse;
import com.sage.teachingassistant.domain.MessageDirection;
import com.sage.teachingassistant.service.LearnerService;
import com.sage.teachingassistant.service.MessageLogService;
import com.sage.teachingassistant.telegram.TelegramMessenger;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lets anything outside the bot push a message into a Telegram chat — the seam a
 * separate tutoring service would use once the teaching logic moves out of process.
 */
@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final ObjectProvider<TelegramMessenger> messengerProvider;
    private final LearnerService learnerService;
    private final MessageLogService messageLogService;

    public MessageController(ObjectProvider<TelegramMessenger> messengerProvider,
                             LearnerService learnerService,
                             MessageLogService messageLogService) {
        this.messengerProvider = messengerProvider;
        this.learnerService = learnerService;
        this.messageLogService = messageLogService;
    }

    @PostMapping
    public ResponseEntity<SendMessageResponse> send(@Valid @RequestBody SendMessageRequest request) {
        TelegramMessenger messenger = messengerProvider.getIfAvailable();
        if (messenger == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Telegram bot is not configured. Set TELEGRAM_BOT_TOKEN and restart.");
        }

        Integer telegramMessageId = messenger.sendText(request.chatId(), request.text());
        boolean delivered = telegramMessageId != null;

        // Only log against a learner we already know about; a send to an unknown
        // chat would otherwise need a synthetic row.
        boolean logged = learnerService.findByTelegramChatId(request.chatId())
                .map(learner -> {
                    messageLogService.record(
                            learner,
                            MessageDirection.OUTBOUND,
                            request.text(),
                            telegramMessageId == null ? null : telegramMessageId.longValue());
                    return true;
                })
                .orElse(false);

        return ResponseEntity.ok(
                new SendMessageResponse(request.chatId(), telegramMessageId, delivered, logged));
    }
}
