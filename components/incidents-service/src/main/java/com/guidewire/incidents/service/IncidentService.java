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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class IncidentService {

    private static final Logger LOG = Logger.getLogger(IncidentService.class);

    @Inject
    IncidentRepository incidentRepository;

    @Inject
    IncidentMapper incidentMapper;

    @Inject
    IncidentEventProducer incidentEventProducer;

    @Transactional
    public IncidentResponse create(CreateIncidentRequest request) {
        LOG.infof("Creating incident for claimId=%s, customerId=%s", request.getClaimId(), request.getCustomerId());

        Incident incident = incidentMapper.toEntity(request);

        if (request.getPriority() == null) {
            incident.setPriority(Priority.MEDIUM);
        }

        incidentRepository.persist(incident);
        LOG.infof("Incident created with id=%s", incident.getId());
        return incidentMapper.toResponse(incident);
    }

    public IncidentResponse findById(UUID id) {
        Incident incident = incidentRepository.findByIdOptional(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", id));
        return incidentMapper.toResponse(incident);
    }

    public PagedResponse<IncidentResponse> findAll(UUID claimId, UUID customerId, IncidentStatus status,
                                                    Priority priority, int pageIndex, int pageSize) {
        List<Incident> incidents = incidentRepository.findWithFilters(
                claimId, customerId, status, priority, pageIndex, pageSize);
        long totalElements = incidentRepository.countWithFilters(claimId, customerId, status, priority);
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        List<IncidentResponse> content = incidentMapper.toResponseList(incidents);

        return PagedResponse.<IncidentResponse>builder()
                .content(content)
                .pageIndex(pageIndex)
                .pageSize(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(pageIndex < totalPages - 1)
                .hasPrevious(pageIndex > 0)
                .build();
    }

    public IncidentResponse update(UUID id, UpdateIncidentRequest request) {
        UpdateResult result = doUpdate(id, request);

        if (result.statusChanged) {
            incidentEventProducer.publishStatusChanged(
                    result.incident,
                    result.previousStatus,
                    "incidents-service",
                    request.getResolution()
            );
        }

        return incidentMapper.toResponse(result.incident);
    }

    @Transactional
    UpdateResult doUpdate(UUID id, UpdateIncidentRequest request) {
        Incident incident = incidentRepository.findByIdOptional(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", id));

        IncidentStatus previousStatus = incident.getStatus();
        boolean statusChanged = false;

        if (request.getStatus() != null) {
            IncidentStatus targetStatus = request.getStatus();

            if (!previousStatus.canTransitionTo(targetStatus)) {
                throw new InvalidStatusTransitionException(previousStatus.name(), targetStatus.name());
            }
            incident.setStatus(targetStatus);
            statusChanged = true;
            LOG.infof("Incident %s status changed from %s to %s", id, previousStatus, targetStatus);
        }

        if (request.getPriority() != null) {
            incident.setPriority(request.getPriority());
        }

        if (request.getTitle() != null) {
            incident.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            incident.setDescription(request.getDescription());
        }

        if (request.getAssignedTo() != null) {
            incident.setAssignedTo(request.getAssignedTo());
        }

        if (request.getResolution() != null) {
            incident.setResolution(request.getResolution());
        }

        incidentRepository.persist(incident);

        return new UpdateResult(incident, previousStatus, statusChanged);
    }

    static class UpdateResult {
        final Incident incident;
        final IncidentStatus previousStatus;
        final boolean statusChanged;

        UpdateResult(Incident incident, IncidentStatus previousStatus, boolean statusChanged) {
            this.incident = incident;
            this.previousStatus = previousStatus;
            this.statusChanged = statusChanged;
        }
    }
}
