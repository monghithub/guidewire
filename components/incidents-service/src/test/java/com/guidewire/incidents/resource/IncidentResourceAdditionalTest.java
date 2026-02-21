package com.guidewire.incidents.resource;

import com.guidewire.incidents.dto.IncidentResponse;
import com.guidewire.incidents.dto.PagedResponse;
import com.guidewire.incidents.dto.UpdateIncidentRequest;
import com.guidewire.incidents.entity.IncidentStatus;
import com.guidewire.incidents.entity.Priority;
import com.guidewire.incidents.exception.InvalidStatusTransitionException;
import com.guidewire.incidents.exception.ResourceNotFoundException;
import com.guidewire.incidents.service.IncidentService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@QuarkusTest
@Disabled("Requires PostgreSQL and Kafka — run in CI with Testcontainers or in OpenShift")
class IncidentResourceAdditionalTest {

    @InjectMock
    IncidentService incidentService;

    private UUID incidentId;
    private UUID claimId;
    private UUID customerId;
    private IncidentResponse sampleResponse;

    @BeforeEach
    void setUp() {
        incidentId = UUID.randomUUID();
        claimId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        sampleResponse = IncidentResponse.builder()
                .id(incidentId)
                .claimId(claimId)
                .customerId(customerId)
                .status(IncidentStatus.OPEN)
                .priority(Priority.MEDIUM)
                .title("Test incident title")
                .description("Test incident description with enough chars")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void patchIncident_shouldReturn200_withUpdatedFields() {
        IncidentResponse updatedResponse = IncidentResponse.builder()
                .id(incidentId)
                .claimId(claimId)
                .customerId(customerId)
                .status(IncidentStatus.IN_PROGRESS)
                .priority(Priority.HIGH)
                .title("Updated incident title")
                .description("Test incident description with enough chars")
                .assignedTo("agent-007")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(incidentService.update(eq(incidentId), any(UpdateIncidentRequest.class)))
                .thenReturn(updatedResponse);

        given()
                .contentType("application/json")
                .body("""
                        {
                            "status": "IN_PROGRESS",
                            "priority": "HIGH",
                            "title": "Updated incident title",
                            "assignedTo": "agent-007"
                        }
                        """)
                .when()
                .patch("/api/v1/incidents/{id}", incidentId)
                .then()
                .statusCode(200)
                .body("id", equalTo(incidentId.toString()))
                .body("status", equalTo("IN_PROGRESS"))
                .body("priority", equalTo("HIGH"))
                .body("title", equalTo("Updated incident title"))
                .body("assignedTo", equalTo("agent-007"));
    }

    @Test
    void getIncident_shouldReturn404_whenNotFound() {
        UUID missingId = UUID.randomUUID();
        when(incidentService.findById(missingId))
                .thenThrow(new ResourceNotFoundException("Incident", missingId));

        given()
                .when()
                .get("/api/v1/incidents/{id}", missingId)
                .then()
                .statusCode(404)
                .body("title", equalTo("Resource Not Found"))
                .body("status", equalTo(404))
                .body("detail", notNullValue());
    }

    @Test
    void patchIncident_shouldReturn409_whenInvalidStatusTransition() {
        when(incidentService.update(eq(incidentId), any(UpdateIncidentRequest.class)))
                .thenThrow(new InvalidStatusTransitionException("CLOSED", "OPEN"));

        given()
                .contentType("application/json")
                .body("""
                        {
                            "status": "OPEN"
                        }
                        """)
                .when()
                .patch("/api/v1/incidents/{id}", incidentId)
                .then()
                .statusCode(409)
                .body("title", equalTo("Invalid Status Transition"))
                .body("status", equalTo(409))
                .body("detail", notNullValue());
    }

    @Test
    void createIncident_shouldReturn400_whenClaimIdMissing() {
        given()
                .contentType("application/json")
                .body("""
                        {
                            "customerId": "%s",
                            "title": "Valid title here",
                            "description": "A sufficiently long description for validation"
                        }
                        """.formatted(customerId))
                .when()
                .post("/api/v1/incidents")
                .then()
                .statusCode(400);
    }

    @Test
    void createIncident_shouldReturn400_whenTitleTooShort() {
        given()
                .contentType("application/json")
                .body("""
                        {
                            "claimId": "%s",
                            "customerId": "%s",
                            "title": "Ab",
                            "description": "A sufficiently long description for validation"
                        }
                        """.formatted(claimId, customerId))
                .when()
                .post("/api/v1/incidents")
                .then()
                .statusCode(400);
    }

    @Test
    void listIncidents_shouldFilterByStatus() {
        PagedResponse<IncidentResponse> pagedResponse = PagedResponse.<IncidentResponse>builder()
                .content(List.of(sampleResponse))
                .pageIndex(0)
                .pageSize(20)
                .totalElements(1L)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(incidentService.findAll(any(), any(), eq(IncidentStatus.OPEN), any(), anyInt(), anyInt()))
                .thenReturn(pagedResponse);

        given()
                .queryParam("status", "OPEN")
                .when()
                .get("/api/v1/incidents")
                .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].status", equalTo("OPEN"))
                .body("totalElements", equalTo(1));
    }
}
