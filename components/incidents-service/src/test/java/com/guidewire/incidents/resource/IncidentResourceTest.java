package com.guidewire.incidents.resource;

import com.guidewire.incidents.dto.CreateIncidentRequest;
import com.guidewire.incidents.dto.IncidentResponse;
import com.guidewire.incidents.dto.PagedResponse;
import com.guidewire.incidents.entity.IncidentStatus;
import com.guidewire.incidents.entity.Priority;
import com.guidewire.incidents.service.IncidentService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
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
class IncidentResourceTest {

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
    void createIncident_shouldReturn201() {
        when(incidentService.create(any(CreateIncidentRequest.class))).thenReturn(sampleResponse);

        given()
                .contentType("application/json")
                .body("""
                        {
                            "claimId": "%s",
                            "customerId": "%s",
                            "priority": "MEDIUM",
                            "title": "Test incident title",
                            "description": "Test incident description with enough chars"
                        }
                        """.formatted(claimId, customerId))
                .when()
                .post("/api/v1/incidents")
                .then()
                .statusCode(201)
                .body("id", equalTo(incidentId.toString()))
                .body("claimId", equalTo(claimId.toString()))
                .body("customerId", equalTo(customerId.toString()))
                .body("status", equalTo("OPEN"))
                .body("priority", equalTo("MEDIUM"));
    }

    @Test
    void getIncident_shouldReturn200() {
        when(incidentService.findById(incidentId)).thenReturn(sampleResponse);

        given()
                .when()
                .get("/api/v1/incidents/{id}", incidentId)
                .then()
                .statusCode(200)
                .body("id", equalTo(incidentId.toString()))
                .body("title", equalTo("Test incident title"));
    }

    @Test
    void listIncidents_shouldReturn200() {
        PagedResponse<IncidentResponse> pagedResponse = PagedResponse.<IncidentResponse>builder()
                .content(List.of(sampleResponse))
                .pageIndex(0)
                .pageSize(20)
                .totalElements(1L)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(incidentService.findAll(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(pagedResponse);

        given()
                .when()
                .get("/api/v1/incidents")
                .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].id", equalTo(incidentId.toString()))
                .body("pageIndex", equalTo(0))
                .body("pageSize", equalTo(20))
                .body("totalElements", equalTo(1))
                .body("totalPages", equalTo(1));
    }
}
