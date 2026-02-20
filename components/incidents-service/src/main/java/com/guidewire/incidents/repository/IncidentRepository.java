package com.guidewire.incidents.repository;

import com.guidewire.incidents.entity.Incident;
import com.guidewire.incidents.entity.IncidentStatus;
import com.guidewire.incidents.entity.Priority;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class IncidentRepository implements PanacheRepositoryBase<Incident, UUID> {

    public List<Incident> findByClaimId(UUID claimId) {
        return list("claimId", claimId);
    }

    public List<Incident> findByCustomerId(UUID customerId) {
        return list("customerId", customerId);
    }

    public List<Incident> findByStatus(IncidentStatus status) {
        return list("status", status);
    }

    public List<Incident> findByPriority(Priority priority) {
        return list("priority", priority);
    }

    public Optional<Incident> findByIdOptional(UUID id) {
        return find("id", id).firstResultOptional();
    }

    public List<Incident> findWithFilters(UUID claimId, UUID customerId, IncidentStatus status,
                                          Priority priority, int pageIndex, int pageSize) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        if (claimId != null) {
            query.append(" AND claimId = :claimId");
            params.put("claimId", claimId);
        }
        if (customerId != null) {
            query.append(" AND customerId = :customerId");
            params.put("customerId", customerId);
        }
        if (status != null) {
            query.append(" AND status = :status");
            params.put("status", status);
        }
        if (priority != null) {
            query.append(" AND priority = :priority");
            params.put("priority", priority);
        }

        return find(query.toString(), Sort.by("createdAt").descending(), params)
                .page(Page.of(pageIndex, pageSize))
                .list();
    }

    public long countWithFilters(UUID claimId, UUID customerId, IncidentStatus status, Priority priority) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        if (claimId != null) {
            query.append(" AND claimId = :claimId");
            params.put("claimId", claimId);
        }
        if (customerId != null) {
            query.append(" AND customerId = :customerId");
            params.put("customerId", customerId);
        }
        if (status != null) {
            query.append(" AND status = :status");
            params.put("status", status);
        }
        if (priority != null) {
            query.append(" AND priority = :priority");
            params.put("priority", priority);
        }

        return count(query.toString(), params);
    }
}
