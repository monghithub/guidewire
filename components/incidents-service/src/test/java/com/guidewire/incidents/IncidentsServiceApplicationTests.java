package com.guidewire.incidents;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
class IncidentsServiceApplicationTests {

    @Test
    void applicationStartsSuccessfully() {
        given()
                .when().get("/q/health/ready")
                .then()
                .statusCode(200);
    }
}
