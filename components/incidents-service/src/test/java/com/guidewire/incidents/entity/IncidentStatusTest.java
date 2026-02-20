package com.guidewire.incidents.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentStatusTest {

    // --- Valid transitions ---

    static Stream<Arguments> validTransitions() {
        return Stream.of(
                // OPEN -> {IN_PROGRESS, ESCALATED, CLOSED}
                Arguments.of(IncidentStatus.OPEN, IncidentStatus.IN_PROGRESS),
                Arguments.of(IncidentStatus.OPEN, IncidentStatus.ESCALATED),
                Arguments.of(IncidentStatus.OPEN, IncidentStatus.CLOSED),
                // IN_PROGRESS -> {RESOLVED, ESCALATED}
                Arguments.of(IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED),
                Arguments.of(IncidentStatus.IN_PROGRESS, IncidentStatus.ESCALATED),
                // RESOLVED -> {CLOSED, IN_PROGRESS}
                Arguments.of(IncidentStatus.RESOLVED, IncidentStatus.CLOSED),
                Arguments.of(IncidentStatus.RESOLVED, IncidentStatus.IN_PROGRESS),
                // ESCALATED -> {IN_PROGRESS, CLOSED}
                Arguments.of(IncidentStatus.ESCALATED, IncidentStatus.IN_PROGRESS),
                Arguments.of(IncidentStatus.ESCALATED, IncidentStatus.CLOSED)
        );
    }

    @ParameterizedTest(name = "{0} -> {1} should be allowed")
    @MethodSource("validTransitions")
    void canTransitionTo_shouldReturnTrue_forValidTransitions(IncidentStatus from, IncidentStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    // --- Invalid transitions ---

    static Stream<Arguments> invalidTransitions() {
        return Stream.of(
                // OPEN cannot go to OPEN or RESOLVED
                Arguments.of(IncidentStatus.OPEN, IncidentStatus.OPEN),
                Arguments.of(IncidentStatus.OPEN, IncidentStatus.RESOLVED),
                // IN_PROGRESS cannot go to OPEN, IN_PROGRESS, or CLOSED
                Arguments.of(IncidentStatus.IN_PROGRESS, IncidentStatus.OPEN),
                Arguments.of(IncidentStatus.IN_PROGRESS, IncidentStatus.IN_PROGRESS),
                Arguments.of(IncidentStatus.IN_PROGRESS, IncidentStatus.CLOSED),
                // RESOLVED cannot go to OPEN, RESOLVED, or ESCALATED
                Arguments.of(IncidentStatus.RESOLVED, IncidentStatus.OPEN),
                Arguments.of(IncidentStatus.RESOLVED, IncidentStatus.RESOLVED),
                Arguments.of(IncidentStatus.RESOLVED, IncidentStatus.ESCALATED),
                // CLOSED cannot go anywhere
                Arguments.of(IncidentStatus.CLOSED, IncidentStatus.OPEN),
                Arguments.of(IncidentStatus.CLOSED, IncidentStatus.IN_PROGRESS),
                Arguments.of(IncidentStatus.CLOSED, IncidentStatus.RESOLVED),
                Arguments.of(IncidentStatus.CLOSED, IncidentStatus.CLOSED),
                Arguments.of(IncidentStatus.CLOSED, IncidentStatus.ESCALATED),
                // ESCALATED cannot go to OPEN, RESOLVED, or ESCALATED
                Arguments.of(IncidentStatus.ESCALATED, IncidentStatus.OPEN),
                Arguments.of(IncidentStatus.ESCALATED, IncidentStatus.RESOLVED),
                Arguments.of(IncidentStatus.ESCALATED, IncidentStatus.ESCALATED)
        );
    }

    @ParameterizedTest(name = "{0} -> {1} should be rejected")
    @MethodSource("invalidTransitions")
    void canTransitionTo_shouldReturnFalse_forInvalidTransitions(IncidentStatus from, IncidentStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @Test
    void closed_shouldHaveNoValidTransitions() {
        for (IncidentStatus target : IncidentStatus.values()) {
            assertThat(IncidentStatus.CLOSED.canTransitionTo(target))
                    .as("CLOSED -> %s should be false", target)
                    .isFalse();
        }
    }
}
