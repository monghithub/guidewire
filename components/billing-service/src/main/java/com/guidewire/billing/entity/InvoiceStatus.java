package com.guidewire.billing.entity;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum InvoiceStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED;

    private static final Map<InvoiceStatus, Set<InvoiceStatus>> ALLOWED_TRANSITIONS = Map.of(
            PENDING, EnumSet.of(PROCESSING, CANCELLED),
            PROCESSING, EnumSet.of(COMPLETED, FAILED),
            COMPLETED, EnumSet.noneOf(InvoiceStatus.class),
            FAILED, EnumSet.of(PENDING),
            CANCELLED, EnumSet.noneOf(InvoiceStatus.class)
    );

    public boolean canTransitionTo(InvoiceStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(InvoiceStatus.class)).contains(target);
    }
}
