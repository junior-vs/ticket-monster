package br.vsjr.labs.ticketmonster.catalog.adapter.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.NotImplementedException;

@Path("/api/v1/admin/event-categories")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ROLE_ADMIN")
public class EventCategoryAdminResource {

    @GET
    public String test(){
        return notimplemented();
    }

    private String notimplemented() {
         throw new NotImplementedException("not implememented");

    }

}