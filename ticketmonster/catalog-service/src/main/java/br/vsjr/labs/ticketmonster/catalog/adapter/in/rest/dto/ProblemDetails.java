package br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Representacao de erro no padrao RFC 7807 (Problem Details).
 *
 * <p>Contrato: schema {@code ProblemDetails} de
 * {@code contracts/event-categories-api.yaml}; media type de resposta
 * {@code application/problem+json} (FR-005).
 *
 * @param type     URI de identificacao do tipo de problema.
 * @param title    resumo legivel e estavel do problema.
 * @param status   codigo de status HTTP.
 * @param detail   explicacao especifica da ocorrencia.
 * @param instance URI da ocorrencia (opcional).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetails(String type, String title, int status, String detail, String instance) {
}
