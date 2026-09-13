package com.sage.teachingassistant.telegram;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CommandRouterTest {

    private final CommandRouter router = new CommandRouter("SageBot");

    @Test
    void recognisesStart() {
        Optional<String> reply = router.replyFor("/start", 42L, "Anmol");

        assertThat(reply).isPresent();
        assertThat(reply.get()).contains("Anmol").contains("Sage");
    }

    @Test
    void fallsBackToAGenericGreetingWhenNoNameIsKnown() {
        Optional<String> reply = router.replyFor("/start", 42L, null);

        assertThat(reply).isPresent();
        assertThat(reply.get()).contains("Hello.");
    }

    @Test
    void handlesCommandsCaseInsensitively() {
        assertThat(router.replyFor("/HELP", 1L, null)).isPresent();
        assertThat(router.replyFor("/About", 1L, null)).isPresent();
    }

    @Test
    void stripsTheAddressedBotSuffix() {
        assertThat(router.replyFor("/help@SageBot", 1L, null)).isPresent();
    }

    @Test
    void ignoresCommandsAddressedToADifferentBot() {
        assertThat(router.replyFor("/help@OtherBot", 1L, null)).isEmpty();
    }

    @Test
    void ignoresSurroundingWhitespaceAndArguments() {
        assertThat(router.replyFor("  /help   me please ", 1L, null)).isPresent();
    }

    @Test
    void returnsEmptyForOrdinaryText() {
        assertThat(router.replyFor("what is a derivative?", 1L, null)).isEmpty();
        assertThat(router.replyFor("", 1L, null)).isEmpty();
        assertThat(router.replyFor(null, 1L, null)).isEmpty();
    }

    @Test
    void returnsEmptyForUnknownCommands() {
        assertThat(router.replyFor("/nonsense", 1L, null)).isEmpty();
    }

    @Test
    void reportsTheChatId() {
        Optional<String> reply = router.replyFor("/id", 987654L, null);

        assertThat(reply).isPresent();
        assertThat(reply.get()).contains("987654");
    }

    @Test
    void isKnownCommandMatchesOnlyRealCommands() {
        assertThat(router.isKnownCommand("/start")).isTrue();
        assertThat(router.isKnownCommand("hello")).isFalse();
    }
}
