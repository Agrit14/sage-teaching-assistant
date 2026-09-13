package com.sage.teachingassistant.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordApprovalClassifierTest {

    private final KeywordApprovalClassifier classifier = new KeywordApprovalClassifier();

    @ParameterizedTest
    @ValueSource(strings = {
            "yes", "Yes", "YES", "y", "yep", "yeah", "ok", "okay", "sure",
            "correct", "right", "approved", "go ahead", "proceed", "next",
            "looks good", "lgtm", "perfect", "no changes"
    })
    void acceptsPlainApprovals(String message) {
        assertThat(classifier.isApproval(message)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"yes!", "yes.", " yes ", "yes,", "looks good!!"})
    void toleratesPunctuationAndWhitespace(String message) {
        assertThat(classifier.isApproval(message)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "yes but add more about mitosis",
            "looks good, though the second section is thin",
            "no",
            "not yet",
            "change the title",
            "make it shorter",
            "add more detail on the light reactions",
            "actually make it about respiration",
            "I'm not sure",
            ""
    })
    void treatsEverythingElseAsFeedback(String message) {
        assertThat(classifier.isApproval(message)).isFalse();
    }

    @Test
    void rejectsNull() {
        assertThat(classifier.isApproval(null)).isFalse();
    }

    @Test
    void isStrictAboutApprovalWithTrailingContent() {
        // The whole point: a bare "yes" approves, "yes and also..." does not.
        assertThat(classifier.isApproval("yes")).isTrue();
        assertThat(classifier.isApproval("yes and also change the title")).isFalse();
    }
}
