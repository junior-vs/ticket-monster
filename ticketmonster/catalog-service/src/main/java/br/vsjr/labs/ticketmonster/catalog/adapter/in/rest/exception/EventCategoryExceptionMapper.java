package br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.dto.ProblemDetails;
import br.vsjr.labs.ticketmonster.catalog.domain.exception.EventCategoryAlreadyExistsException;
import br.vsjr.labs.ticketmonster.catalog.domain.exception.EventCategoryInUseException;
import br.vsjr.labs.ticketmonster.catalog.domain.exception.EventCategoryInvalidDescriptionException;
import br.vsjr.labs.ticketmonster.catalog.domain.exception.EventCategoryNotFoundException;

/**
 * Traduz as excecoes de dominio de {@code EventCategory} para respostas HTTP no
 * padrao RFC 7807 (Problem Details).
 *
 * <p>Mapa de status (research.md secao 4, FR-005):
 * <ul>
 *   <li>{@code 400 Bad Request} - descricao nula, em branco ou acima de 120 caracteres.</li>
 *   <li>{@code 404 Not Found} - id de categoria inexistente.</li>
 *   <li>{@code 409 Conflict} - descricao duplicada (RN06) ou categoria em uso (RN04).</li>
 * </ul>
 */
@Provider
public class EventCategoryExceptionMapper {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String TYPE_VALIDATION = "https://ticketmonster.com/errors/validation";
    private static final String TYPE_NOT_FOUND = "https://ticketmonster.com/errors/not-found";
    private static final String TYPE_CONFLICT = "https://ticketmonster.com/errors/conflict";

    @ServerExceptionMapper
    public Response mapInvalidDescription(EventCategoryInvalidDescriptionException ex) {
        return problem(Response.Status.BAD_REQUEST, TYPE_VALIDATION, "Descrição Inválida", ex.getMessage());
    }

    @ServerExceptionMapper
    public Response mapNotFound(EventCategoryNotFoundException ex) {
        return problem(Response.Status.NOT_FOUND, TYPE_NOT_FOUND, "Categoria Não Encontrada", ex.getMessage());
    }

    @ServerExceptionMapper
    public Response mapAlreadyExists(EventCategoryAlreadyExistsException ex) {
        return problem(Response.Status.CONFLICT, TYPE_CONFLICT, "Categoria Duplicada", ex.getMessage());
    }

    @ServerExceptionMapper
    public Response mapInUse(EventCategoryInUseException ex) {
        return problem(Response.Status.CONFLICT, TYPE_CONFLICT, "Categoria Em Uso", ex.getMessage());
    }

    private Response problem(Response.Status status, String type, String title, String detail) {
        ProblemDetails body = new ProblemDetails(type, title, status.getStatusCode(), detail, null);
        return Response.status(status).type(PROBLEM_JSON).entity(body).build();
    }
}
