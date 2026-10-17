package br.vsjr.labs.ticketmonster.catalog.adapter.rest;


import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.util.List;

@Path("/api/v1/event-categories")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EventCategoryResource {




    @GET
    @Operation(summary = "Listar categorias de evento publicamente")
    @APIResponse(responseCode = "200", description = "Categorias retornadas com sucesso")
    public Uni<List<CategoryResponse>> listCategories(@QueryParam("page") Integer page, @QueryParam("size") Integer size) {
        // Implement the logic to retrieve event categories from the database or service
        // For demonstration purposes, we'll return a static list of categories
        List<CategoryResponse> categories = List.of(
                new CategoryResponse()
        );

        return Uni.createFrom().item(categories);
 
    }
}
