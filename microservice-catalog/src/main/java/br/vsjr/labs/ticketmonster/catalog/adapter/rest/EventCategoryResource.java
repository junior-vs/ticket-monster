package br.vsjr.labs.ticketmonster.catalog.adapter.rest;


import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.NotImplementedException;

@Path("/api/v1/event-categories")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EventCategoryResource {



    @GET
    public String test(){
        return notimplemented();
    }

    private String notimplemented() {
        throw new NotImplementedException("not implememented");

    }
}
