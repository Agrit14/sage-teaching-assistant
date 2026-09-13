package com.sage.teachingassistant.api;

import com.sage.teachingassistant.api.dto.LearnerResponse;
import com.sage.teachingassistant.api.dto.MessageResponse;
import com.sage.teachingassistant.domain.Learner;
import com.sage.teachingassistant.service.LearnerService;
import com.sage.teachingassistant.service.MessageLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Read access to the people Sage talks to, and to what was said. */
@RestController
@RequestMapping("/api/v1/learners")
public class LearnerController {

    private final LearnerService learnerService;
    private final MessageLogService messageLogService;

    public LearnerController(LearnerService learnerService, MessageLogService messageLogService) {
        this.learnerService = learnerService;
        this.messageLogService = messageLogService;
    }

    @GetMapping
    public List<LearnerResponse> list() {
        return learnerService.findAll().stream()
                .map(LearnerResponse::from)
                .toList();
    }

    @GetMapping("/{chatId}")
    public LearnerResponse get(@PathVariable long chatId) {
        return learnerService.findByTelegramChatId(chatId)
                .map(LearnerResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No learner with chat id " + chatId));
    }

    @GetMapping("/{chatId}/messages")
    public Page<MessageResponse> messages(@PathVariable long chatId,
                                          @PageableDefault(size = 50) Pageable pageable) {
        Learner learner = learnerService.findByTelegramChatId(chatId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No learner with chat id " + chatId));

        return messageLogService.history(learner.getId(), pageable).map(MessageResponse::from);
    }
}
