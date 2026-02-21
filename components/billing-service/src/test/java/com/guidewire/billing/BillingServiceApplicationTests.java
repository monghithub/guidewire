package com.guidewire.billing;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Requires PostgreSQL and Kafka — run in CI with Testcontainers or in OpenShift")
class BillingServiceApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring application context starts successfully
    }
}
