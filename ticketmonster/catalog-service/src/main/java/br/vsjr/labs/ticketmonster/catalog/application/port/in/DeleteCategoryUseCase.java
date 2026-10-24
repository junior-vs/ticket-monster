package br.vsjr.labs.ticketmonster.catalog.application.port.in;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

/**
 * Caso de uso de exclusao de uma categoria de evento (US-CAT-09 / P3).
 *
 * <p>Porta de entrada implementada pelo servico de aplicacao
 * {@code EventCategoryService}.
 */
public interface DeleteCategoryUseCase {

    /**
     * Exclui uma categoria que nao possua eventos associados.
     *
     * <p>Regras: RN04 / FR-004 (categoria em uso nao pode ser removida),
     * FR-007a (invalidacao do cache de listagem) e 404 quando o id nao existe.
     *
     * @param id identificador da categoria a excluir
     * @return conclusao reativa sem conteudo
     */
    Uni<Void> delete(UUID id);
}
