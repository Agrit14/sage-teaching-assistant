package com.sage.teachingassistant.telegram;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramMessengerTest {

    @Test
    void leavesShortTextAlone() {
        List<String> chunks = TelegramMessenger.split("hello");

        assertThat(chunks).containsExactly("hello");
    }

    @Test
    void keepsTextExactlyAtTheLimitAsOneChunk() {
        String text = "a".repeat(TelegramMessenger.MAX_MESSAGE_LENGTH);

        assertThat(TelegramMessenger.split(text)).hasSize(1);
    }

    @Test
    void splitsTextOverTheLimit() {
        String text = "a".repeat(TelegramMessenger.MAX_MESSAGE_LENGTH + 1);

        List<String> chunks = TelegramMessenger.split(text);

        assertThat(chunks).hasSize(2);
        assertThat(chunks).allSatisfy(chunk ->
                assertThat(chunk.length()).isLessThanOrEqualTo(TelegramMessenger.MAX_MESSAGE_LENGTH));
    }

    @Test
    void prefersBreakingOnANewline() {
        String firstLine = "x".repeat(3000);
        String secondLine = "y".repeat(3000);

        List<String> chunks = TelegramMessenger.split(firstLine + "\n" + secondLine);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0)).isEqualTo(firstLine);
        assertThat(chunks.get(1)).isEqualTo(secondLine);
    }

    @Test
    void neverLosesCharactersWhenSplittingOnSpaces() {
        String text = ("word ".repeat(2000)).strip();

        List<String> chunks = TelegramMessenger.split(text);

        String rejoined = String.join(" ", chunks);
        assertThat(rejoined).isEqualTo(text);
        assertThat(chunks).allSatisfy(chunk ->
                assertThat(chunk.length()).isLessThanOrEqualTo(TelegramMessenger.MAX_MESSAGE_LENGTH));
    }
}
