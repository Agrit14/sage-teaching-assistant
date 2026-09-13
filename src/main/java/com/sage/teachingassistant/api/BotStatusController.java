package com.sage.teachingassistant.api;

import com.sage.teachingassistant.api.dto.BotStatusResponse;
import com.sage.teachingassistant.config.SageTelegramProperties;
import com.sage.teachingassistant.service.LearnerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Quick way to confirm the bot side is actually running. */
@RestController
@RequestMapping("/api/v1/bot")
public class BotStatusController {

    private final SageTelegramProperties properties;
    private final LearnerService learnerService;

    public BotStatusController(SageTelegramProperties properties, LearnerService learnerService) {
        this.properties = properties;
        this.learnerService = learnerService;
    }

    @GetMapping("/status")
    public BotStatusResponse status() {
        return new BotStatusResponse(
                properties.isActive(),
                properties.username(),
                learnerService.count());
    }
}
