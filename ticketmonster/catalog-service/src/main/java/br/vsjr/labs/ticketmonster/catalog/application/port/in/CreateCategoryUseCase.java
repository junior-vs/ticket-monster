package br.vsjr.labs.ticketmonster.catalog.application.port.in;

import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategory;
import io.smallrye.mutiny.Uni;

/**
 * Caso de uso de cadastro de uma nova categoria de evento (US-CAT-09 / P1).
 *
 * <p>Porta de entrada implementada pelo servico de aplicacao
 * {@code EventCategoryService}.
 */
public interface CreateCategoryUseCase {

    /**
     * Cadastra uma nova categoria de evento com descricao unica.
     *
     * <p>Regras: RN06 / FR-002 (descricao obrigatoria e unica), FR-002a
     * (normalizacao por {@code trim} antes de validar e comparar), FR-003 (limite
     * de 120 caracteres) e FR-007a (invalidacao do cache de listagem).
     *
     * @param rawDescription descricao bruta informada pelo administrador
     * @return a categoria persistida, com UUID v4 e timestamp de criacao
     */
    Uni<EventCategory> create(String rawDescription);
}
