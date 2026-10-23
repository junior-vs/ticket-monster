package br.vsjr.labs.ticketmonster.catalog.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import br.vsjr.labs.ticketmonster.catalog.application.port.in.CreateCategoryUseCase;
import br.vsjr.labs.ticketmonster.catalog.application.port.out.EventCategoryCachePort;
import br.vsjr.labs.ticketmonster.catalog.application.port.out.EventCategoryRepositoryPort;
import br.vsjr.labs.ticketmonster.catalog.domain.exception.EventCategoryAlreadyExistsException;
import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategory;
import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategoryDescription;
import io.smallrye.mutiny.Uni;

/**
 * Caso de uso de cadastro de categoria de evento (US-CAT-09 / P1).
 *
 * <p>Orquestra o dominio puro ({@link EventCategory}) e as portas de saida de
 * persistencia e cache, sem depender de detalhes de framework.
 *
 * <p>O limite transacional da operacao e demarcado no adaptador de entrada
 * ({@code @WithTransaction} no resource), mantendo esta camada agnostica de
 * persistencia.
 */
@ApplicationScoped
public class EventCategoryService implements CreateCategoryUseCase {

    private static final Logger LOG = Logger.getLogger(EventCategoryService.class);

    private final EventCategoryRepositoryPort repositoryPort;
    private final EventCategoryCachePort cachePort;

    @Inject
    public EventCategoryService(EventCategoryRepositoryPort repositoryPort, EventCategoryCachePort cachePort) {
        this.repositoryPort = repositoryPort;
        this.cachePort = cachePort;
    }

    @Override
    public Uni<EventCategory> create(String rawDescription) {
        return Uni.createFrom().deferred(() -> {
            // FR-002a - normalizacao (trim) antes de validar e antes de comparar unicidade
            EventCategoryDescription description = EventCategoryDescription.of(rawDescription); // FR-002 / FR-003
            return repositoryPort.existsByDescription(description.value())                        // RN06
                    .chain(exists -> Boolean.TRUE.equals(exists)
                            ? Uni.createFrom().failure(new EventCategoryAlreadyExistsException(description.value()))
                            : repositoryPort.save(EventCategory.create(description)))
                    // FR-007a / ADR-009 - invalidacao do cache apos a escrita
                    .call(saved -> invalidateCategoryList());
        });
    }

    /**
     * Invalida o cache da listagem apos uma escrita bem-sucedida (FR-007a).
     *
     * <p>ADR-009 - falhas do Redis nao devem interromper a resposta de sucesso da
     * escrita relacional; a falha e registrada e absorvida.
     */
    private Uni<Void> invalidateCategoryList() {
        return cachePort.invalidateCategoryList()
                .onFailure().invoke(failure -> LOG.warnf(failure,
                        "Falha ao invalidar o cache da listagem de categorias apos escrita: %s", failure.getMessage()))
                .onFailure().recoverWithItem((Void) null);
    }
}
