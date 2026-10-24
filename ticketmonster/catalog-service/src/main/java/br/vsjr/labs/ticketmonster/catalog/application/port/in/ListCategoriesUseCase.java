package br.vsjr.labs.ticketmonster.catalog.application.port.in;

import java.util.List;

import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategory;
import io.smallrye.mutiny.Uni;

/**
 * Caso de uso de listagem das categorias de eventos (US-CAT-09 / P4).
 *
 * <p>Porta de entrada implementada pelo servico de aplicacao
 * {@code EventCategoryService}.
 */
public interface ListCategoriesUseCase {

    /**
     * Lista todas as categorias ordenadas alfabeticamente pela descricao.
     *
     * <p>Regras: FR-007 (cache-aside sob a chave {@code catalog:categories:list}).
     *
     * @return categorias em ordem ascendente de descricao
     */
    Uni<List<EventCategory>> listAll();

    Uni<EventCategory> getByDescription(String description);
}
