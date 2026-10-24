package br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.exception;

import java.util.Objects;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.dto.ProblemDetails;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

/**
 * Traduz falhas de Jakarta Bean Validation (ADR 08) para o padrao RFC 7807.
 *
 * <p>Sem este mapeamento o framework responderia com a representacao padrao de
 * violacoes, que nao segue o contrato de erro unico do servico (FR-005).
 */
@Provider
public class ConstraintViolationExceptionMapper {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String TYPE_VALIDATION = "https://ticketmonster.com/errors/validation";

    @ServerExceptionMapper
    public Response mapConstraintViolation(ConstraintViolationException ex, UriInfo uriInfo) {
        String detail = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Requisição inválida.");
        ProblemDetails body = new ProblemDetails(TYPE_VALIDATION, "Requisição Inválida",
                Response.Status.BAD_REQUEST.getStatusCode(), detail, instance(uriInfo));
        return Response.status(Response.Status.BAD_REQUEST).type(PROBLEM_JSON).entity(body).build();
    }

    private static String instance(UriInfo uriInfo) {
        String path = uriInfo.getPath();
        return path.startsWith("/") ? path : "/" + path;
    }
}
