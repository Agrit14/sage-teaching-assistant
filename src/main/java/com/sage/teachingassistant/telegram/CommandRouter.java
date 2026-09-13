package com.sage.teachingassistant.telegram;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Turns an incoming Telegram message into a reply.
 *
 * <p>Deliberately free of Spring and of the persistence layer so the wording and
 * the command parsing can be unit-tested without a database.
 */
public class CommandRouter {

    private static final List<String> KNOWN_COMMANDS =
            List.of("/start", "/help", "/about", "/subjects", "/id");

    private final String botUsername;

    public CommandRouter(String botUsername) {
        this.botUsername = botUsername == null ? "" : botUsername;
    }

    /**
     * Returns Sage's reply, or empty when the text is not a command and should be
     * handled as ordinary conversation.
     */
    public Optional<String> replyFor(String text, long chatId, String displayName) {
        Optional<String> command = commandOf(text);
        if (command.isEmpty()) {
            return Optional.empty();
        }

        return switch (command.get()) {
            case "/start" -> Optional.of(welcome(displayName));
            case "/help" -> Optional.of(help());
            case "/about" -> Optional.of(about());
            case "/subjects" -> Optional.of(subjects());
            case "/id" -> Optional.of("Your Telegram chat id is " + chatId
                    + ". Useful when wiring Sage up to something else.");
            default -> Optional.empty();
        };
    }

    /** True when the text looks like a bot command addressed to Sage. */
    public boolean isKnownCommand(String text) {
        return commandOf(text).isPresent();
    }

    /**
     * Normalises {@code /Help@SageBot now} down to {@code /help}.
     */
    private Optional<String> commandOf(String text) {
        if (text == null) {
            return Optional.empty();
        }

        String trimmed = text.strip();
        if (!trimmed.startsWith("/")) {
            return Optional.empty();
        }

        String firstToken = trimmed.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);

        int at = firstToken.indexOf('@');
        if (at >= 0) {
            String addressedTo = firstToken.substring(at + 1);
            // Ignore commands aimed at a different bot sharing this chat.
            if (!botUsername.isBlank() && !botUsername.equalsIgnoreCase(addressedTo)) {
                return Optional.empty();
            }
            firstToken = firstToken.substring(0, at);
        }

        return KNOWN_COMMANDS.contains(firstToken) ? Optional.of(firstToken) : Optional.empty();
    }

    private String welcome(String displayName) {
        String greeting = displayName == null || displayName.isBlank()
                ? "Hello."
                : "Hello, " + displayName + ".";

        return """
                %s

                I'm Sage, your teaching assistant. I'm here to help you actually \
                understand things, not just hand you answers.

                Tell me what you're working on and we'll take it from there. \
                Send /help to see what I can do.""".formatted(greeting);
    }

    private String help() {
        return """
                Here's what I can do right now:

                /start - introduce myself and get set up
                /subjects - see what I can teach
                /id - show your Telegram chat id
                /about - what Sage is and how it works
                /help - this message

                Or just send me a question in plain language. More teaching \
                features are on the way.""";
    }

    private String about() {
        return """
                Sage is a teaching assistant.

                The idea is simple: a good teacher doesn't just produce the answer, \
                they find the gap in your understanding and close it. So I'll ask \
                questions, check your reasoning, and adapt rather than lecture.

                Built as a Telegram bot on a Spring Boot backend with PostgreSQL, \
                so I can remember what you've covered and where you got stuck.""";
    }

    private String subjects() {
        return """
                Subjects are still being wired up. The plan is to cover:

                - Mathematics
                - Physics
                - Computer science and programming
                - English language and writing

                Tell me what you'd like to start with and I'll take note of it.""";
    }
}
