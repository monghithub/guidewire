package com.guidewire.incidents.entity;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum IncidentStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    ESCALATED;

    private static final Map<IncidentStatus, Set<IncidentStatus>> ALLOWED_TRANSITIONS = Map.of(
            OPEN, EnumSet.of(IN_PROGRESS, ESCALATED, CLOSED),
            IN_PROGRESS, EnumSet.of(RESOLVED, ESCALATED),
            RESOLVED, EnumSet.of(CLOSED, IN_PROGRESS),
            CLOSED, EnumSet.noneOf(IncidentStatus.class),
            ESCALATED, EnumSet.of(IN_PROGRESS, CLOSED)
    );

    public boolean canTransitionTo(IncidentStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(IncidentStatus.class)).contains(target);
    }
}
