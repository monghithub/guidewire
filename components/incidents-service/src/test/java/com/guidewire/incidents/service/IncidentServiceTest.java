package com.guidewire.incidents.service;

import com.guidewire.incidents.dto.CreateIncidentRequest;
import com.guidewire.incidents.dto.IncidentResponse;
import com.guidewire.incidents.dto.PagedResponse;
import com.guidewire.incidents.dto.UpdateIncidentRequest;
import com.guidewire.incidents.entity.Incident;
import com.guidewire.incidents.entity.IncidentStatus;
import com.guidewire.incidents.entity.Priority;
import com.guidewire.incidents.exception.InvalidStatusTransitionException;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

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
    void create_shouldCreateIncident() {
        CreateIncidentRequest request = new CreateIncidentRequest(
                claimId,
                customerId,
                Priority.HIGH,
                "New incident",
                "Detailed description of the incident",
                null,
                null
        );

        when(incidentMapper.toEntity(request)).thenReturn(sampleIncident);
        doNothing().when(incidentRepository).persist(any(Incident.class));
        when(incidentMapper.toResponse(sampleIncident)).thenReturn(sampleResponse);

        IncidentResponse result = incidentService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(incidentId);
        assertThat(result.claimId()).isEqualTo(claimId);
        assertThat(result.customerId()).isEqualTo(customerId);

        verify(incidentMapper).toEntity(request);
        verify(incidentRepository).persist(sampleIncident);
        verify(incidentMapper).toResponse(sampleIncident);
    }

    @Test
    void create_shouldSetDefaultPriority_whenPriorityIsNull() {
        CreateIncidentRequest request = new CreateIncidentRequest(
                claimId,
                customerId,
                null,
                "New incident",
                "Detailed description of the incident",
                null,
                null
        );

        Incident incidentWithoutPriority = Incident.builder()
                .claimId(claimId)
                .customerId(customerId)
                .title("New incident")
                .description("Detailed description of the incident")
                .build();

        when(incidentMapper.toEntity(request)).thenReturn(incidentWithoutPriority);
        doNothing().when(incidentRepository).persist(any(Incident.class));
        when(incidentMapper.toResponse(incidentWithoutPriority)).thenReturn(sampleResponse);

        incidentService.create(request);

        assertThat(incidentWithoutPriority.getPriority()).isEqualTo(Priority.MEDIUM);
        verify(incidentRepository).persist(incidentWithoutPriority);
    }

    @Test
    void findById_shouldReturnIncident_whenExists() {
        when(incidentRepository.findByIdOptional(incidentId)).thenReturn(Optional.of(sampleIncident));
        when(incidentMapper.toResponse(sampleIncident)).thenReturn(sampleResponse);

        IncidentResponse result = incidentService.findById(incidentId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(incidentId);
        verify(incidentRepository).findByIdOptional(incidentId);
        verify(incidentMapper).toResponse(sampleIncident);
    }

    @Test
    void findById_shouldThrow_whenNotFound() {
        UUID missingId = UUID.randomUUID();
        when(incidentRepository.findByIdOptional(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.findById(missingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Incident")
                .hasMessageContaining(missingId.toString());

        verify(incidentMapper, never()).toResponse(any());
    }

    @Test
    void update_shouldTransitionFromOpenToInProgress() {
        UpdateIncidentRequest request = new UpdateIncidentRequest(
                IncidentStatus.IN_PROGRESS,
                null,
                null,
                null,
                "agent-001",
                null
        );

        when(incidentRepository.findByIdOptional(incidentId)).thenReturn(Optional.of(sampleIncident));
        doNothing().when(incidentRepository).persist(any(Incident.class));
        when(incidentMapper.toResponse(sampleIncident)).thenReturn(
                new IncidentResponse(
                        incidentId,
                        null,
                        null,
                        IncidentStatus.IN_PROGRESS,
                        null,
                        null,
                        null,
                        "agent-001",
                        null,
                        null,
                        null,
                        null
                )
        );

        IncidentResponse result = incidentService.update(incidentId, request);

        assertThat(result).isNotNull();
        assertThat(sampleIncident.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(sampleIncident.getAssignedTo()).isEqualTo("agent-001");

        verify(incidentRepository).persist(sampleIncident);
        verify(incidentEventProducer).publishStatusChanged(
                eq(sampleIncident),
                eq(IncidentStatus.OPEN),
                eq("incidents-service"),
                any()
        );
    }

    @Test
    void update_shouldThrow_whenInvalidTransition() {
        Incident closedIncident = Incident.builder()
                .id(incidentId)
                .claimId(claimId)
                .customerId(customerId)
                .status(IncidentStatus.CLOSED)
                .priority(Priority.MEDIUM)
                .title("Closed incident")
                .description("This incident is already closed")
                .build();

        UpdateIncidentRequest request = new UpdateIncidentRequest(
                IncidentStatus.OPEN,
                null,
                null,
                null,
                null,
                null
        );

        when(incidentRepository.findByIdOptional(incidentId)).thenReturn(Optional.of(closedIncident));

        assertThatThrownBy(() -> incidentService.update(incidentId, request))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("CLOSED")
                .hasMessageContaining("OPEN");

        verify(incidentRepository, never()).persist(any(Incident.class));
        verify(incidentEventProducer, never()).publishStatusChanged(any(), any(), anyString(), anyString());
    }

    @Test
    void update_shouldNotPublishEvent_whenStatusNotChanged() {
        UpdateIncidentRequest request = new UpdateIncidentRequest(
                null,
                null,
                "Updated title only",
                null,
                null,
                null
        );

        when(incidentRepository.findByIdOptional(incidentId)).thenReturn(Optional.of(sampleIncident));
        doNothing().when(incidentRepository).persist(any(Incident.class));
        when(incidentMapper.toResponse(sampleIncident)).thenReturn(sampleResponse);

        incidentService.update(incidentId, request);

        assertThat(sampleIncident.getTitle()).isEqualTo("Updated title only");
        verify(incidentEventProducer, never()).publishStatusChanged(any(), any(), anyString(), anyString());
    }

    @Test
    void findAll_shouldReturnPagedResponse() {
        List<Incident> incidents = List.of(sampleIncident);
        List<IncidentResponse> responses = List.of(sampleResponse);

        when(incidentRepository.findWithFilters(null, null, null, null, 0, 20))
                .thenReturn(incidents);
        when(incidentRepository.countWithFilters(null, null, null, null))
                .thenReturn(1L);
        when(incidentMapper.toResponseList(incidents)).thenReturn(responses);

        PagedResponse<IncidentResponse> result = incidentService.findAll(null, null, null, null, 0, 20);

        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.pageIndex()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(1L);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();

        verify(incidentRepository).findWithFilters(null, null, null, null, 0, 20);
        verify(incidentRepository).countWithFilters(null, null, null, null);
        verify(incidentMapper).toResponseList(incidents);
    }

    @Test
    void findAll_shouldReturnPagedResponse_withMultiplePages() {
        List<Incident> incidents = List.of(sampleIncident);
        List<IncidentResponse> responses = List.of(sampleResponse);

        when(incidentRepository.findWithFilters(null, null, null, null, 1, 10))
                .thenReturn(incidents);
        when(incidentRepository.countWithFilters(null, null, null, null))
                .thenReturn(25L);
        when(incidentMapper.toResponseList(incidents)).thenReturn(responses);

        PagedResponse<IncidentResponse> result = incidentService.findAll(null, null, null, null, 1, 10);

        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isTrue();
    }
}
