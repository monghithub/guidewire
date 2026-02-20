package com.guidewire.billing.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceStatusTest {

    @Nested
    @DisplayName("Valid transitions")
    class ValidTransitions {

        @Test
        @DisplayName("PENDING can transition to PROCESSING")
        void pending_canTransitionTo_processing() {
            assertThat(InvoiceStatus.PENDING.canTransitionTo(InvoiceStatus.PROCESSING)).isTrue();
        }

        @Test
        @DisplayName("PENDING can transition to CANCELLED")
        void pending_canTransitionTo_cancelled() {
            assertThat(InvoiceStatus.PENDING.canTransitionTo(InvoiceStatus.CANCELLED)).isTrue();
        }

        @Test
        @DisplayName("PROCESSING can transition to COMPLETED")
        void processing_canTransitionTo_completed() {
            assertThat(InvoiceStatus.PROCESSING.canTransitionTo(InvoiceStatus.COMPLETED)).isTrue();
        }

        @Test
        @DisplayName("PROCESSING can transition to FAILED")
        void processing_canTransitionTo_failed() {
            assertThat(InvoiceStatus.PROCESSING.canTransitionTo(InvoiceStatus.FAILED)).isTrue();
        }

        @Test
        @DisplayName("FAILED can transition to PENDING")
        void failed_canTransitionTo_pending() {
            assertThat(InvoiceStatus.FAILED.canTransitionTo(InvoiceStatus.PENDING)).isTrue();
        }
    }

    @Nested
    @DisplayName("Invalid transitions")
    class InvalidTransitions {

        static Stream<Arguments> invalidTransitions() {
            return Stream.of(
                    // PENDING cannot go to COMPLETED, FAILED, or itself
                    Arguments.of(InvoiceStatus.PENDING, InvoiceStatus.PENDING),
                    Arguments.of(InvoiceStatus.PENDING, InvoiceStatus.COMPLETED),
                    Arguments.of(InvoiceStatus.PENDING, InvoiceStatus.FAILED),

                    // PROCESSING cannot go to PENDING, CANCELLED, or itself
                    Arguments.of(InvoiceStatus.PROCESSING, InvoiceStatus.PROCESSING),
                    Arguments.of(InvoiceStatus.PROCESSING, InvoiceStatus.PENDING),
                    Arguments.of(InvoiceStatus.PROCESSING, InvoiceStatus.CANCELLED),

                    // FAILED cannot go to PROCESSING, COMPLETED, CANCELLED, or itself
                    Arguments.of(InvoiceStatus.FAILED, InvoiceStatus.FAILED),
                    Arguments.of(InvoiceStatus.FAILED, InvoiceStatus.PROCESSING),
                    Arguments.of(InvoiceStatus.FAILED, InvoiceStatus.COMPLETED),
                    Arguments.of(InvoiceStatus.FAILED, InvoiceStatus.CANCELLED)
            );
        }

        @ParameterizedTest(name = "{0} -> {1} should be invalid")
        @MethodSource("invalidTransitions")
        @DisplayName("Should reject invalid transitions")
        void shouldRejectInvalidTransitions(InvoiceStatus from, InvoiceStatus to) {
            assertThat(from.canTransitionTo(to)).isFalse();
        }
    }

    @Nested
    @DisplayName("Terminal states")
    class TerminalStates {

        @ParameterizedTest(name = "COMPLETED cannot transition to {0}")
        @EnumSource(InvoiceStatus.class)
        @DisplayName("COMPLETED has no valid transitions")
        void completed_hasNoTransitions(InvoiceStatus target) {
            assertThat(InvoiceStatus.COMPLETED.canTransitionTo(target)).isFalse();
        }

        @ParameterizedTest(name = "CANCELLED cannot transition to {0}")
        @EnumSource(InvoiceStatus.class)
        @DisplayName("CANCELLED has no valid transitions")
        void cancelled_hasNoTransitions(InvoiceStatus target) {
            assertThat(InvoiceStatus.CANCELLED.canTransitionTo(target)).isFalse();
        }
    }
}
