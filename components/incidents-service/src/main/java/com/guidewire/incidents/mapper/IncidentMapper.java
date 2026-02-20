package com.guidewire.incidents.mapper;

import com.guidewire.incidents.dto.CreateIncidentRequest;
import com.guidewire.incidents.dto.IncidentResponse;
import com.guidewire.incidents.entity.Incident;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "cdi")
public interface IncidentMapper {

    IncidentResponse toResponse(Incident incident);

    List<IncidentResponse> toResponseList(List<Incident> incidents);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "resolution", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Incident toEntity(CreateIncidentRequest request);
}
