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
        LOG.infof("Creating incident for claimId=%s, customerId=%s", request.claimId(), request.customerId());

        Incident incident = incidentMapper.toEntity(request);

        if (request.priority() == null) {
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

        return new PagedResponse<>(
                content,
                pageIndex,
                pageSize,
                totalElements,
                totalPages,
                pageIndex < totalPages - 1,
                pageIndex > 0
        );
    }

    public IncidentResponse update(UUID id, UpdateIncidentRequest request) {
        UpdateResult result = doUpdate(id, request);

        if (result.statusChanged()) {
            incidentEventProducer.publishStatusChanged(
                    result.incident(),
                    result.previousStatus(),
                    "incidents-service",
                    request.resolution()
            );
        }

        return incidentMapper.toResponse(result.incident());
    }

    @Transactional
    UpdateResult doUpdate(UUID id, UpdateIncidentRequest request) {
        Incident incident = incidentRepository.findByIdOptional(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", id));

        IncidentStatus previousStatus = incident.getStatus();
        boolean statusChanged = false;

        if (request.status() != null) {
            IncidentStatus targetStatus = request.status();

            if (!previousStatus.canTransitionTo(targetStatus)) {
                throw new InvalidStatusTransitionException(previousStatus.name(), targetStatus.name());
            }
            incident.setStatus(targetStatus);
            statusChanged = true;
            LOG.infof("Incident %s status changed from %s to %s", id, previousStatus, targetStatus);
        }

        if (request.priority() != null) {
            incident.setPriority(request.priority());
        }

        if (request.title() != null) {
            incident.setTitle(request.title());
        }

        if (request.description() != null) {
            incident.setDescription(request.description());
        }

        if (request.assignedTo() != null) {
            incident.setAssignedTo(request.assignedTo());
        }

        if (request.resolution() != null) {
            incident.setResolution(request.resolution());
        }

        incidentRepository.persist(incident);

        return new UpdateResult(incident, previousStatus, statusChanged);
    }

    record UpdateResult(Incident incident, IncidentStatus previousStatus, boolean statusChanged) {}
}
