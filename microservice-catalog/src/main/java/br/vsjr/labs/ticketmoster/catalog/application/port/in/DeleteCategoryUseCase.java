package br.vsjr.labs.ticketmoster.catalog.application.port.in;

import br.vsjr.labs.ticketmoster.catalog.domain.model.EventCategoryId;
import io.smallrye.mutiny.Uni;

public interface DeleteCategoryUseCase {

    public Uni<Void> deleteCategory(EventCategoryId id);
}
