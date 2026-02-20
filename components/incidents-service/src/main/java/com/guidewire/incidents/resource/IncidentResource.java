package com.guidewire.incidents.resource;

import com.guidewire.incidents.dto.CreateIncidentRequest;
import com.guidewire.incidents.dto.IncidentResponse;
import com.guidewire.incidents.dto.PagedResponse;
import com.guidewire.incidents.dto.UpdateIncidentRequest;
import com.guidewire.incidents.entity.IncidentStatus;
import com.guidewire.incidents.entity.Priority;
import com.guidewire.incidents.service.IncidentService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/api/v1/incidents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IncidentResource {

    @Inject
    IncidentService incidentService;

    @GET
    public Response findAll(
            @QueryParam("claimId") UUID claimId,
            @QueryParam("customerId") UUID customerId,
            @QueryParam("status") IncidentStatus status,
            @QueryParam("priority") Priority priority,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {

        PagedResponse<IncidentResponse> response = incidentService.findAll(
                claimId, customerId, status, priority, page, size);
        return Response.ok(response).build();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") UUID id) {
        IncidentResponse response = incidentService.findById(id);
        return Response.ok(response).build();
    }

    @POST
    public Response create(@Valid CreateIncidentRequest request) {
        IncidentResponse response = incidentService.create(request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @PATCH
    @Path("/{id}")
    public Response update(@PathParam("id") UUID id, @Valid UpdateIncidentRequest request) {
        IncidentResponse response = incidentService.update(id, request);
        return Response.ok(response).build();
    }
}
