package com.guidewire.rules.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ClaimFactTest {

    @Test
    void addFlaggedReason_lazyInitializesListAndAddsReason() {
        ClaimFact claim = new ClaimFact();
        claim.setFlaggedReasons(null); // force null to test lazy init

        claim.addFlaggedReason("Suspicious pattern detected");

        assertThat(claim.getFlaggedReasons())
                .isNotNull()
                .containsExactly("Suspicious pattern detected");
    }

    @Test
    void addFlaggedReason_multipleReasonsAccumulate() {
        ClaimFact claim = ClaimFact.builder()
                .claimId("CLM-001")
                .build();

        claim.addFlaggedReason("Reason A");
        claim.addFlaggedReason("Reason B");
        claim.addFlaggedReason("Reason C");

        assertThat(claim.getFlaggedReasons())
                .hasSize(3)
                .containsExactly("Reason A", "Reason B", "Reason C");
    }

    @Test
    void daysSinceRegistration_normalCase() {
        ClaimFact claim = ClaimFact.builder()
                .customerRegistrationDate(LocalDate.of(2025, 1, 1))
                .incidentDate(LocalDate.of(2025, 1, 31))
                .build();

        long days = claim.daysSinceRegistration();

        assertThat(days).isEqualTo(30);
    }

    @Test
    void daysSinceRegistration_returnsMaxValue_whenIncidentDateIsNull() {
        ClaimFact claim = ClaimFact.builder()
                .customerRegistrationDate(LocalDate.of(2025, 1, 1))
                .incidentDate(null)
                .build();

        long days = claim.daysSinceRegistration();

        assertThat(days).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void daysSinceRegistration_returnsMaxValue_whenRegistrationDateIsNull() {
        ClaimFact claim = ClaimFact.builder()
                .customerRegistrationDate(null)
                .incidentDate(LocalDate.of(2025, 6, 1))
                .build();

        long days = claim.daysSinceRegistration();

        assertThat(days).isEqualTo(Long.MAX_VALUE);
    }
}
