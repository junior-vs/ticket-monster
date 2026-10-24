package br.vsjr.labs.ticketmonster.catalog.application.port.in;

import java.util.UUID;

import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategory;
import io.smallrye.mutiny.Uni;

/**
 * Caso de uso de alteracao da descricao de uma categoria existente (US-CAT-09 / P2).
 *
 * <p>Porta de entrada implementada pelo servico de aplicacao
 * {@code EventCategoryService}.
 */
public interface UpdateCategoryUseCase {

    /**
     * Altera a descricao de uma categoria existente.
     *
     * <p>Regras: RN06 / FR-002a (unicidade apos normalizacao, ignorando a propria
     * categoria), FR-003 (limite de 120 caracteres), FR-007a (invalidacao do cache
     * de listagem) e 404 quando o id nao existe.
     *
     * @param id identificador da categoria a alterar
     * @param rawDescription nova descricao bruta informada pelo administrador
     * @return a categoria atualizada
     */
    Uni<EventCategory> update(UUID id, String rawDescription);
}
