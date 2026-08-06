package br.vsjr.labs.ticketmonster.catalog.adapter.rest;

import br.vsjr.labs.ticketmonster.catalog.adapter.out.dto.CategoryResponse;
import br.vsjr.labs.ticketmonster.catalog.application.mapper.EventCategoryMapper;
import br.vsjr.labs.ticketmonster.catalog.application.port.in.ListCategoriesUseCase;
import br.vsjr.labs.ticketmonster.catalog.domain.vo.PageRequest;
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

    private final ListCategoriesUseCase listCategoriesUseCase;
    private final EventCategoryMapper mapper;

    public EventCategoryResource(ListCategoriesUseCase listCategoriesUseCase, EventCategoryMapper mapper) {
        this.listCategoriesUseCase = listCategoriesUseCase;
        this.mapper = mapper;
    }

    @GET
    @Operation(summary = "Listar categorias de evento publicamente")
    @APIResponse(responseCode = "200", description = "Categorias retornadas com sucesso")
    public Uni<List<CategoryResponse>> listCategories(@QueryParam("page") Integer page, @QueryParam("size") Integer size) {
        var pageRequest = PageRequest.of(page, size);
        return listCategoriesUseCase.listAllCategories(pageRequest)
                .onItem().transform(categories -> categories.stream()
                        .map(mapper::toResponse)
                        .toList());
    }
}
