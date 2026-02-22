package com.guidewire.incidents.dto;

import java.util.List;

public record PagedResponse<T>(
        List<T> content,
        int pageIndex,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious) {}
