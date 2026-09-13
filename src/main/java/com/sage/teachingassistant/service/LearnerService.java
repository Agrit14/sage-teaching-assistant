package com.sage.teachingassistant.service;

import com.sage.teachingassistant.domain.Learner;
import com.sage.teachingassistant.repository.LearnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LearnerService {

    private final LearnerRepository learnerRepository;

    public LearnerService(LearnerRepository learnerRepository) {
        this.learnerRepository = learnerRepository;
    }

    /**
     * Looks up the learner behind a Telegram chat, creating them on first contact.
     *
     * <p>Profile fields are only overwritten when Telegram actually sends a value,
     * so a learner who hides their username later does not lose it in our records.
     */
    public Learner findOrCreate(long telegramChatId,
                                Long telegramUserId,
                                String username,
                                String firstName,
                                String lastName,
                                String languageCode) {

        Learner learner = learnerRepository.findByTelegramChatId(telegramChatId)
                .orElseGet(() -> new Learner(telegramChatId));

        if (telegramUserId != null) {
            learner.setTelegramUserId(telegramUserId);
        }
        if (username != null && !username.isBlank()) {
            learner.setUsername(username);
        }
        if (firstName != null && !firstName.isBlank()) {
            learner.setFirstName(firstName);
        }
        if (lastName != null && !lastName.isBlank()) {
            learner.setLastName(lastName);
        }
        if (languageCode != null && !languageCode.isBlank()) {
            learner.setLanguageCode(languageCode);
        }

        Instant now = Instant.now();
        learner.setLastSeenAt(now);
        learner.setUpdatedAt(now);

        return learnerRepository.save(learner);
    }

    @Transactional(readOnly = true)
    public Optional<Learner> findByTelegramChatId(long telegramChatId) {
        return learnerRepository.findByTelegramChatId(telegramChatId);
    }

    @Transactional(readOnly = true)
    public List<Learner> findAll() {
        return learnerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public long count() {
        return learnerRepository.count();
    }
}
