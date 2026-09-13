package com.sage.teachingassistant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** A single message exchanged with a learner, kept for history and context. */
@Entity
@Table(name = "messages")
public class MessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "learner_id", nullable = false)
    private Learner learner;

    @Column(name = "telegram_message_id")
    private Long telegramMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 16)
    private MessageDirection direction;

    @Column(name = "text", length = 4096)
    private String text;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MessageLog() {
        // for JPA
    }

    public MessageLog(Learner learner, MessageDirection direction, String text, Long telegramMessageId) {
        this.learner = learner;
        this.direction = direction;
        this.text = text;
        this.telegramMessageId = telegramMessageId;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Learner getLearner() {
        return learner;
    }

    public Long getTelegramMessageId() {
        return telegramMessageId;
    }

    public MessageDirection getDirection() {
        return direction;
    }

    public String getText() {
        return text;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
