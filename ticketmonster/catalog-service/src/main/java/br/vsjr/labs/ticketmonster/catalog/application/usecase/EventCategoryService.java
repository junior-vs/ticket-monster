package br.vsjr.labs.ticketmonster.catalog.application.usecase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import br.vsjr.labs.ticketmonster.catalog.application.port.in.CreateCategoryUseCase;
import br.vsjr.labs.ticketmonster.catalog.application.port.in.DeleteCategoryUseCase;
import br.vsjr.labs.ticketmonster.catalog.application.port.in.ListCategoriesUseCase;
import br.vsjr.labs.ticketmonster.catalog.application.port.in.UpdateCategoryUseCase;
import br.vsjr.labs.ticketmonster.catalog.application.port.out.EventCategoryCachePort;
import br.vsjr.labs.ticketmonster.catalog.application.port.out.EventCategoryRepositoryPort;
import br.vsjr.labs.ticketmonster.catalog.application.port.out.EventRepositoryPort;
import br.vsjr.labs.ticketmonster.catalog.domain.exception.EventCategoryAlreadyExistsException;
import br.vsjr.labs.ticketmonster.catalog.domain.exception.EventCategoryInUseException;
import br.vsjr.labs.ticketmonster.catalog.domain.exception.EventCategoryNotFoundException;
import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategory;
import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategoryDescription;
import io.smallrye.mutiny.Uni;

/**
 * Servico de aplicacao dos casos de uso de categoria de evento (US-CAT-09).
 *
 * <p>Orquestra o dominio puro ({@link EventCategory}) e as portas de saida de
 * persistencia e cache, sem depender de detalhes de framework.
 *
 * <p>O limite transacional das operacoes de escrita e demarcado no adaptador de
 * entrada ({@code @WithTransaction} no resource), mantendo esta camada agnostica
 * de persistencia.
 */
@ApplicationScoped
public class EventCategoryService
        implements CreateCategoryUseCase, UpdateCategoryUseCase, DeleteCategoryUseCase, ListCategoriesUseCase {

    private static final Logger LOG = Logger.getLogger(EventCategoryService.class);

    private final EventCategoryRepositoryPort repositoryPort;
    private final EventRepositoryPort eventRepositoryPort;
    private final EventCategoryCachePort cachePort;

    @Inject
    public EventCategoryService(EventCategoryRepositoryPort repositoryPort,
            EventRepositoryPort eventRepositoryPort,
            EventCategoryCachePort cachePort) {

        this.repositoryPort = repositoryPort;
        this.eventRepositoryPort = eventRepositoryPort;
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

    @Override
    public Uni<EventCategory> update(UUID id, String rawDescription) {
        return Uni.createFrom().deferred(() -> {
            EventCategoryDescription description = EventCategoryDescription.of(rawDescription); // FR-002a / FR-003
            return repositoryPort.findByIdOrEmpty(id)
                    .map(found -> found.orElseThrow(() -> new EventCategoryNotFoundException(id)))
                    .chain(category -> repositoryPort.existsByDescriptionAndIdNot(description.value(), id) // RN06
                            .chain(duplicated -> {
                                if (Boolean.TRUE.equals(duplicated)) {
                                    return Uni.createFrom().<EventCategory>failure(
                                            new EventCategoryAlreadyExistsException(description.value()));
                                }
                                category.changeDescription(description);
                                return repositoryPort.save(category);
                            }))
                    .call(saved -> invalidateCategoryList()); // FR-007a
        });
    }

    @Override
    public Uni<Void> delete(UUID id) {
        return Uni.createFrom().deferred(() -> repositoryPort.findByIdOrEmpty(id)
                .map(found -> found.orElseThrow(() -> new EventCategoryNotFoundException(id)))
                .chain(category -> eventRepositoryPort.existsByCategoryId(id) // RN04 / FR-004
                        .chain(inUse -> Boolean.TRUE.equals(inUse)
                                ? Uni.createFrom().<Boolean>failure(new EventCategoryInUseException())
                                : repositoryPort.removeById(id)))
                .replaceWithVoid()
                .call(removed -> invalidateCategoryList())); // FR-007a
    }

    @Override
    public Uni<List<EventCategory>> listAll() {
        // FR-007 - Cache-Aside: consulta o Redis e, em miss, carrega a base e popula o cache
        return cachePort.getCategoryList()
                .onFailure().invoke(failure -> LOG.warnf(failure,
                        "Falha ao ler o cache da listagem de categorias; consultando a base: %s", failure.getMessage()))
                .onFailure().recoverWithItem(Optional.empty())
                .chain(cached -> cached.isPresent()
                        ? Uni.createFrom().item(cached.get())
                        : loadFromRepository());
    }

    @Override
    public Uni<EventCategory> getByDescription(String description) {
        return repositoryPort.existsByDescription(description) //
                .chain(exists -> Boolean.TRUE.equals(exists)
                        ? repositoryPort.findByDescription(description)
                                .map(found -> found.orElseThrow(
                                        () -> new EventCategoryNotFoundException(description)))
                        : Uni.createFrom().failure(new EventCategoryNotFoundException(description)));
    }

    private Uni<List<EventCategory>> loadFromRepository() {
        return repositoryPort.findAllOrderedByDescription()
                .call(categories -> cachePort.putCategoryList(categories)
                        .onFailure().invoke(failure -> LOG.warnf(failure,
                                "Falha ao armazenar o cache da listagem de categorias: %s", failure.getMessage()))
                        .onFailure().recoverWithItem((Void) null));
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
