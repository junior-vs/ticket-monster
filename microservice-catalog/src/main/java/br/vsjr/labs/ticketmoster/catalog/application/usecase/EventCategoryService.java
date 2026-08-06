package br.vsjr.labs.ticketmoster.catalog.application.usecase;

import br.vsjr.labs.ticketmoster.catalog.application.port.in.CreateCategoryUseCase;
import br.vsjr.labs.ticketmoster.catalog.application.port.in.DeleteCategoryUseCase;
import br.vsjr.labs.ticketmoster.catalog.application.port.in.ListCategoriesUseCase;
import br.vsjr.labs.ticketmoster.catalog.application.port.in.UpdateCategoryUseCase;
import br.vsjr.labs.ticketmoster.catalog.application.port.out.EventCategoryCachePort;
import br.vsjr.labs.ticketmoster.catalog.application.port.out.EventCategoryRepositoryPort;
import br.vsjr.labs.ticketmoster.catalog.domain.exception.EventCategoryAlreadyExistsException;
import br.vsjr.labs.ticketmoster.catalog.domain.exception.EventCategoryNotFoundException;
import br.vsjr.labs.ticketmoster.catalog.domain.model.EventCategoryDescription;
import br.vsjr.labs.ticketmoster.catalog.domain.model.EventCategoryId;
import br.vsjr.labs.ticketmoster.catalog.domain.model.EventCategory;
import br.vsjr.labs.ticketmoster.catalog.domain.vo.PageRequest;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;


// SRP (SOLID): Serviço orquestrador do CRUD administrativo com invalidação de cache
@ApplicationScoped
public class EventCategoryService implements
        CreateCategoryUseCase, UpdateCategoryUseCase, DeleteCategoryUseCase, ListCategoriesUseCase {

    private final EventCategoryRepositoryPort repositoryPort;
    private final EventCategoryCachePort cachePort;
    private final static Logger LOG = LoggerFactory.getLogger(EventCategoryService.class);

    public EventCategoryService(EventCategoryRepositoryPort repositoryPort, EventCategoryCachePort cachePort) {
        this.repositoryPort = repositoryPort;
        this.cachePort = cachePort;
    }


    @Override
    public Uni<EventCategory> createCategory(EventCategory eventCategory) {
        return repositoryPort.existsByNormalizedDescription(eventCategory.description())
                .chain(exists -> {
                            if (exists) {
                                return Uni.createFrom().failure(
                                        new EventCategoryAlreadyExistsException("Já existe uma categoria cadastrada com a descrição: " + eventCategory.description().value())
                                );
                            }
                            return repositoryPort.save(eventCategory)
                                    .chain(savedCategory -> cachePort.invalidateCache().replaceWith(savedCategory));
                        }
                );
    }

    @Override
    public Uni<EventCategory> updateCategory(EventCategoryId id, EventCategoryDescription newDescription) {
        return findExistingCategory(id)
                .chain(existingCategory ->
                        repositoryPort.existsByNormalizedDescriptionAndNotId(newDescription, id)
                                .chain(existsOther -> {
                                    if (existsOther) {
                                        return Uni.createFrom().failure(
                                                new EventCategoryAlreadyExistsException("Já existe outra categoria cadastrada com a descrição: " + newDescription.value())
                                        );
                                    }
                                    var updatedCategory = existingCategory.updateEventCategory(newDescription);
                                    return repositoryPort.save(updatedCategory);
                                })
                )
                .call(cachePort::invalidateCache);
    }

    @Override
    public Uni<Void> deleteCategory(EventCategoryId id) {
        return findExistingCategory(id)
                .chain(ignored -> repositoryPort.hasAssociatedEvents(id))
                .chain(hasAssociatedEvents -> {
                    if (hasAssociatedEvents) {
                        return Uni.createFrom().failure(
                                new IllegalStateException("Não é possível excluir a categoria, pois existem eventos associados a ela.")
                        );
                    }

                    return repositoryPort.deleteById(id)
                            .chain(deleted -> {
                                if (!deleted) {
                                    return Uni.createFrom().failure(
                                            new EventCategoryNotFoundException("Categoria não encontrada para o ID: " + id.value())
                                    );
                                }
                                return cachePort.invalidateCache();
                            });
                });
    }

    private Uni<EventCategory> findExistingCategory(EventCategoryId id) {
        return repositoryPort.findById(id)
                .chain(optionalCategory -> optionalCategory
                        .map(Uni.createFrom()::item)
                        .orElseGet(() -> Uni.createFrom().failure(
                                new EventCategoryNotFoundException("Categoria não encontrada para o ID: " + id.value())
                        )));
    }

    private Uni<Void> invalidateCacheSafe() {
        return cachePort.invalidateCache()
                .onFailure().invoke(err -> LOG.warn("Falha ao invalidar cache de categorias no Redis", err))
                .replaceWithVoid()
                .onFailure().recoverWithItem((Void) null);
    }

    @Override
    public Uni<List<EventCategory>> listAllCategories(PageRequest page) {
        // Cache-Aside Reativo para a listagem pública
        return cachePort.getEventCategoriesList(page)
                .onFailure().recoverWithItem(Optional.empty())
                .chain(cachedOpt -> {
                    if (cachedOpt.isPresent()) {
                        return Uni.createFrom().item(cachedOpt.get());
                    }
                    return repositoryPort.findAllOrderedByDescription()
                            .call(list -> cachePort.putEventCategoryList(list)
                                    .onFailure().invoke(err -> LOG.warn("Falha assíncrona ao salvar categorias no Redis", err))
                                    .replaceWithVoid()
                                    .onFailure().recoverWithItem((Void) null)
                            );
                });
    }
}
