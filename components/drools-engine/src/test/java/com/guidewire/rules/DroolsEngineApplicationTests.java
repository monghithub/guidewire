package com.guidewire.rules;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DroolsEngineApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring context loads successfully with all DRL rules compiled
    }
}
