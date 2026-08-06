package br.vsjr.labs.ticketmoster.catalog.adapter.rest.mapper;

import br.vsjr.labs.ticketmoster.catalog.adapter.out.dto.ProblemDetails;
import br.vsjr.labs.ticketmoster.catalog.domain.exception.*;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class EventCategoryExceptionMapper implements ExceptionMapper<RuntimeException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(RuntimeException exception) {
        if (exception instanceof EventCategoryAlreadyExistsException) {
            return problem(409, "Categoria Duplicada", exception.getMessage(), "https://ticketmonster.com/errors/conflict");
        }
        if (exception instanceof EventCategoryNotFoundException) {
            return problem(404, "Categoria Nao Encontrada", exception.getMessage(), "https://ticketmonster.com/errors/not-found");
        }
        if (exception instanceof EventCategoryInUseException) {
            return problem(409, "Categoria Em Uso", exception.getMessage(), "https://ticketmonster.com/errors/conflict");
        }
        if (exception instanceof InvalidEventCategoryInputException) {
            return problem(400, "Requisicao Invalida", exception.getMessage(), "https://ticketmonster.com/errors/validation");
        }
        if (exception instanceof WebApplicationException webEx) {
            int status = webEx.getResponse().getStatus();
            return problem(status, titleFor(status), webEx.getMessage(), typeFor(status));
        }

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
