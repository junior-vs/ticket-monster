package br.vsjr.labs.ticketmonster.catalog.adapter.rest.mapper;

import br.vsjr.labs.ticketmonster.catalog.adapter.out.dto.ProblemDetails;
import br.vsjr.labs.ticketmonster.catalog.domain.exception.EventCategoryAlreadyExistsException;
import br.vsjr.labs.ticketmonster.catalog.domain.exception.EventCategoryInUseException;
import br.vsjr.labs.ticketmonster.catalog.domain.exception.EventCategoryNotFoundException;
import br.vsjr.labs.ticketmonster.catalog.domain.exception.InvalidEventCategoryInputException;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class EventCategoryExceptionMapper implements ExceptionMapper<RuntimeException> {
    private static final Logger LOG = LoggerFactory.getLogger(EventCategoryExceptionMapper.class);
    public static final String URL_CONFLICT = "https://ticketmonster.com/errors/conflict";
    public static final String URL_VALIDATION = "https://ticketmonster.com/errors/validation";
    public static final String URL_NOT_FOUND = "https://ticketmonster.com/errors/not-found";

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(RuntimeException exception) {
        if (exception instanceof EventCategoryAlreadyExistsException) {
            return problem(409, "Categoria Duplicada", exception.getMessage(), URL_CONFLICT);
        }
        if (exception instanceof EventCategoryNotFoundException) {
            return problem(404, "Categoria Nao Encontrada", exception.getMessage(), URL_NOT_FOUND);
        }
        if (exception instanceof EventCategoryInUseException) {
            return problem(409, "Categoria Em Uso", exception.getMessage(), URL_CONFLICT);
        }
        if (exception instanceof InvalidEventCategoryInputException) {
            return problem(400, "Requisicao Invalida", exception.getMessage(), URL_VALIDATION);
        }
        if (exception instanceof IllegalArgumentException || exception instanceof ValidationException) {
            return problem(400, "Requisicao Invalida", exception.getMessage(), URL_VALIDATION);
        }
        if (exception instanceof WebApplicationException webEx) {
            int status = webEx.getResponse().getStatus();
            return problem(status, titleFor(status), webEx.getMessage(), typeFor(status));
        }

        LOG.error("Unhandled runtime exception while processing event-category request", exception);
        return problem(500, "Erro Interno", "Ocorreu um erro inesperado.", "https://ticketmonster.com/errors/internal");
    }

    private Response problem(int status, String title, String detail, String type) {
        String path = uriInfo != null && uriInfo.getPath() != null ? "/" + uriInfo.getPath() : null;
        ProblemDetails body = new ProblemDetails(type, title, status, detail, path);
        return Response.status(status)
            .type("application/problem+json")
            .entity(body)
            .build();
    }

    private String titleFor(int status) {
        return switch (status) {
            case 400 -> "Requisicao Invalida";
            case 401 -> "Nao Autenticado";
            case 403 -> "Acesso Proibido";
            case 404 -> "Nao Encontrado";
            case 409 -> "Conflito";
            default -> "Erro";
        };
    }

    private String typeFor(int status) {
        return switch (status) {
            case 400 -> "https://ticketmonster.com/errors/validation";
            case 401 -> "https://ticketmonster.com/errors/unauthorized";
            case 403 -> "https://ticketmonster.com/errors/forbidden";
            case 404 -> "https://ticketmonster.com/errors/not-found";
            case 409 -> "https://ticketmonster.com/errors/conflict";
            default -> "https://ticketmonster.com/errors/internal";
        };
    }
}
