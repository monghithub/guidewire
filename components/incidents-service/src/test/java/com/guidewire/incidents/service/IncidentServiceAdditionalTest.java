package com.guidewire.incidents.service;

import com.guidewire.incidents.dto.IncidentResponse;
import com.guidewire.incidents.dto.PagedResponse;
import com.guidewire.incidents.dto.UpdateIncidentRequest;
import com.guidewire.incidents.entity.Incident;
import com.guidewire.incidents.entity.IncidentStatus;
import com.guidewire.incidents.entity.Priority;
import com.guidewire.incidents.exception.ResourceNotFoundException;
import com.guidewire.incidents.kafka.IncidentEventProducer;
import com.guidewire.incidents.mapper.IncidentMapper;
import com.guidewire.incidents.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentServiceAdditionalTest {

    @Mock
    IncidentRepository incidentRepository;

    @Mock
    IncidentMapper incidentMapper;

    @Mock
    IncidentEventProducer incidentEventProducer;

    @InjectMocks
    IncidentService incidentService;

    private UUID incidentId;
    private UUID claimId;
    private UUID customerId;
    private Incident sampleIncident;
    private IncidentResponse sampleResponse;

    @BeforeEach
    void setUp() {
        incidentId = UUID.randomUUID();
        claimId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        sampleIncident = Incident.builder()
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

        sampleResponse = new IncidentResponse(
                incidentId,
                claimId,
                customerId,
                IncidentStatus.OPEN,
                Priority.MEDIUM,
                "Test incident title",
                "Test incident description with enough chars",
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    void update_shouldThrow_whenIncidentNotFound() {
        UUID missingId = UUID.randomUUID();
        UpdateIncidentRequest request = new UpdateIncidentRequest(
                null,
                null,
                "Updated title here",
                null,
                null,
                null
        );

        when(incidentRepository.findByIdOptional(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.update(missingId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Incident")
                .hasMessageContaining(missingId.toString());

        verify(incidentRepository, never()).persist(any(Incident.class));
        verify(incidentEventProducer, never()).publishStatusChanged(any(), any(), anyString(), anyString());
    }

    @Test
    void update_shouldNotPublishEvent_whenOnlyPriorityChanged() {
        UpdateIncidentRequest request = new UpdateIncidentRequest(
                null,
                Priority.CRITICAL,
                null,
                null,
                null,
                null
        );

        when(incidentRepository.findByIdOptional(incidentId)).thenReturn(Optional.of(sampleIncident));
        doNothing().when(incidentRepository).persist(any(Incident.class));
        when(incidentMapper.toResponse(sampleIncident)).thenReturn(sampleResponse);

        IncidentResponse result = incidentService.update(incidentId, request);

        assertThat(result).isNotNull();
        assertThat(sampleIncident.getPriority()).isEqualTo(Priority.CRITICAL);
        verify(incidentEventProducer, never()).publishStatusChanged(any(), any(), anyString(), anyString());
        verify(incidentRepository).persist(sampleIncident);
    }

    @Test
    void update_shouldApplyAllFieldsSimultaneously() {
        UpdateIncidentRequest request = new UpdateIncidentRequest(
                IncidentStatus.IN_PROGRESS,
                Priority.HIGH,
                "Completely new title",
                "Completely new description with enough length",
                "agent-999",
                "Partial resolution note"
        );

        IncidentResponse updatedResponse = new IncidentResponse(
                incidentId,
                null,
                null,
                IncidentStatus.IN_PROGRESS,
                Priority.HIGH,
                "Completely new title",
                "Completely new description with enough length",
                "agent-999",
                "Partial resolution note",
                null,
                null,
                null
        );

        when(incidentRepository.findByIdOptional(incidentId)).thenReturn(Optional.of(sampleIncident));
        doNothing().when(incidentRepository).persist(any(Incident.class));
        when(incidentMapper.toResponse(sampleIncident)).thenReturn(updatedResponse);

        IncidentResponse result = incidentService.update(incidentId, request);

        assertThat(sampleIncident.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(sampleIncident.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(sampleIncident.getTitle()).isEqualTo("Completely new title");
        assertThat(sampleIncident.getDescription()).isEqualTo("Completely new description with enough length");
        assertThat(sampleIncident.getAssignedTo()).isEqualTo("agent-999");
        assertThat(sampleIncident.getResolution()).isEqualTo("Partial resolution note");

        verify(incidentRepository).persist(sampleIncident);
        verify(incidentEventProducer).publishStatusChanged(
                sampleIncident,
                IncidentStatus.OPEN,
                "incidents-service",
                "Partial resolution note"
        );
    }

    @Test
    void update_shouldAllowTransition_openToEscalated() {
        UpdateIncidentRequest request = new UpdateIncidentRequest(
                IncidentStatus.ESCALATED,
                null,
                null,
                null,
                null,
                null
        );

        IncidentResponse escalatedResponse = new IncidentResponse(
                incidentId,
                null,
                null,
                IncidentStatus.ESCALATED,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(incidentRepository.findByIdOptional(incidentId)).thenReturn(Optional.of(sampleIncident));
        doNothing().when(incidentRepository).persist(any(Incident.class));
        when(incidentMapper.toResponse(sampleIncident)).thenReturn(escalatedResponse);

        IncidentResponse result = incidentService.update(incidentId, request);

        assertThat(sampleIncident.getStatus()).isEqualTo(IncidentStatus.ESCALATED);
        verify(incidentEventProducer).publishStatusChanged(
                sampleIncident,
                IncidentStatus.OPEN,
                "incidents-service",
                null
        );
    }

    @Test
    void update_shouldAllowTransition_escalatedToClosed() {
        Incident escalatedIncident = Incident.builder()
                .id(incidentId)
                .claimId(claimId)
                .customerId(customerId)
                .status(IncidentStatus.ESCALATED)
                .priority(Priority.HIGH)
                .title("Escalated incident")
                .description("This incident was escalated")
                .build();

        UpdateIncidentRequest request = new UpdateIncidentRequest(
                IncidentStatus.CLOSED,
                null,
                null,
                null,
                null,
                "Issue resolved after escalation"
        );

        IncidentResponse closedResponse = new IncidentResponse(
                incidentId,
                null,
                null,
                IncidentStatus.CLOSED,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(incidentRepository.findByIdOptional(incidentId)).thenReturn(Optional.of(escalatedIncident));
        doNothing().when(incidentRepository).persist(any(Incident.class));
        when(incidentMapper.toResponse(escalatedIncident)).thenReturn(closedResponse);

        IncidentResponse result = incidentService.update(incidentId, request);

        assertThat(escalatedIncident.getStatus()).isEqualTo(IncidentStatus.CLOSED);
        verify(incidentEventProducer).publishStatusChanged(
                escalatedIncident,
                IncidentStatus.ESCALATED,
                "incidents-service",
                "Issue resolved after escalation"
        );
    }

    @Test
    void update_shouldAllowTransition_resolvedToInProgress() {
        Incident resolvedIncident = Incident.builder()
                .id(incidentId)
                .claimId(claimId)
                .customerId(customerId)
                .status(IncidentStatus.RESOLVED)
                .priority(Priority.MEDIUM)
                .title("Resolved incident")
                .description("This incident was resolved but needs reopening")
                .build();

        UpdateIncidentRequest request = new UpdateIncidentRequest(
                IncidentStatus.IN_PROGRESS,
                null,
                null,
                null,
                "agent-reopen",
                null
        );

        IncidentResponse reopenedResponse = new IncidentResponse(
                incidentId,
                null,
                null,
                IncidentStatus.IN_PROGRESS,
                null,
                null,
                null,
                "agent-reopen",
                null,
                null,
                null,
                null
        );

        when(incidentRepository.findByIdOptional(incidentId)).thenReturn(Optional.of(resolvedIncident));
        doNothing().when(incidentRepository).persist(any(Incident.class));
        when(incidentMapper.toResponse(resolvedIncident)).thenReturn(reopenedResponse);

        IncidentResponse result = incidentService.update(incidentId, request);

        assertThat(resolvedIncident.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(resolvedIncident.getAssignedTo()).isEqualTo("agent-reopen");
        verify(incidentEventProducer).publishStatusChanged(
                resolvedIncident,
                IncidentStatus.RESOLVED,
                "incidents-service",
                null
        );
    }

    @Test
    void findAll_shouldFilterByStatus() {
        List<Incident> incidents = List.of(sampleIncident);
        List<IncidentResponse> responses = List.of(sampleResponse);

        when(incidentRepository.findWithFilters(null, null, IncidentStatus.OPEN, null, 0, 20))
                .thenReturn(incidents);
        when(incidentRepository.countWithFilters(null, null, IncidentStatus.OPEN, null))
                .thenReturn(1L);
        when(incidentMapper.toResponseList(incidents)).thenReturn(responses);

        PagedResponse<IncidentResponse> result = incidentService.findAll(
                null, null, IncidentStatus.OPEN, null, 0, 20);

        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1L);

        verify(incidentRepository).findWithFilters(null, null, IncidentStatus.OPEN, null, 0, 20);
        verify(incidentRepository).countWithFilters(null, null, IncidentStatus.OPEN, null);
    }

    @Test
    void findAll_shouldReturnEmptyResultSet() {
        when(incidentRepository.findWithFilters(null, null, null, null, 0, 20))
                .thenReturn(Collections.emptyList());
        when(incidentRepository.countWithFilters(null, null, null, null))
                .thenReturn(0L);
        when(incidentMapper.toResponseList(Collections.emptyList()))
                .thenReturn(Collections.emptyList());

        PagedResponse<IncidentResponse> result = incidentService.findAll(null, null, null, null, 0, 20);

        assertThat(result).isNotNull();
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0L);
        assertThat(result.totalPages()).isEqualTo(0);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void findAll_shouldCalculatePagesCorrectly_withEdgeCases() {
        // 21 elements with pageSize=10 should yield 3 pages (ceil(21/10) = 3)
        // Requesting page 2 (last page): hasNext=false, hasPrevious=true
        List<Incident> singleIncident = List.of(sampleIncident);
        List<IncidentResponse> singleResponse = List.of(sampleResponse);

        when(incidentRepository.findWithFilters(null, null, null, null, 2, 10))
                .thenReturn(singleIncident);
        when(incidentRepository.countWithFilters(null, null, null, null))
                .thenReturn(21L);
        when(incidentMapper.toResponseList(singleIncident)).thenReturn(singleResponse);

        PagedResponse<IncidentResponse> result = incidentService.findAll(null, null, null, null, 2, 10);

        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.pageIndex()).isEqualTo(2);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isTrue();
    }
}
