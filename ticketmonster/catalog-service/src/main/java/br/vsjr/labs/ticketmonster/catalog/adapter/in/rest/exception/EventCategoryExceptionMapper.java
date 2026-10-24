package br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.exception;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.dto.ProblemDetails;
import br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.mapper.ProblemDetailsMapper;
import br.vsjr.labs.ticketmonster.catalog.domain.exception.EventCategoryAlreadyExistsException;
import br.vsjr.labs.ticketmonster.catalog.domain.exception.EventCategoryInUseException;
import br.vsjr.labs.ticketmonster.catalog.domain.exception.EventCategoryInvalidDescriptionException;
import br.vsjr.labs.ticketmonster.catalog.domain.exception.EventCategoryNotFoundException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

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

    private final ProblemDetailsMapper problemDetailsMapper;

    @Inject
    public EventCategoryExceptionMapper(ProblemDetailsMapper problemDetailsMapper) {
        this.problemDetailsMapper = problemDetailsMapper;
    }

    @ServerExceptionMapper
    public Response mapInvalidDescription(EventCategoryInvalidDescriptionException ex, UriInfo uriInfo) {
        return problem(Response.Status.BAD_REQUEST, TYPE_VALIDATION, "Descrição Inválida", ex.getMessage(), uriInfo);
    }

    @ServerExceptionMapper
    public Response mapNotFound(EventCategoryNotFoundException ex, UriInfo uriInfo) {
        return problem(Response.Status.NOT_FOUND, TYPE_NOT_FOUND, "Categoria Não Encontrada", ex.getMessage(), uriInfo);
    }

    @ServerExceptionMapper
    public Response mapAlreadyExists(EventCategoryAlreadyExistsException ex, UriInfo uriInfo) {
        return problem(Response.Status.CONFLICT, TYPE_CONFLICT, "Categoria Duplicada", ex.getMessage(), uriInfo);
    }

    @ServerExceptionMapper
    public Response mapInUse(EventCategoryInUseException ex, UriInfo uriInfo) {
        return problem(Response.Status.CONFLICT, TYPE_CONFLICT, "Restrição de Integridade Referencial", ex.getMessage(),
                uriInfo);
    }

    private Response problem(Response.Status status, String type, String title, String detail, UriInfo uriInfo) {
        ProblemDetails body = problemDetailsMapper.toProblemDetails(type, title, status.getStatusCode(), detail,
                instance(uriInfo));
        return Response.status(status).type(PROBLEM_JSON).entity(body).build();
    }

    private static String instance(UriInfo uriInfo) {
        String path = uriInfo.getPath();
        return path.startsWith("/") ? path : "/" + path;
    }
}
