package br.vsjr.labs.ticketmonster.catalog.application.port.in;

import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategoryId;
import io.smallrye.mutiny.Uni;

public interface DeleteCategoryUseCase {

    public Uni<Void> deleteCategory(EventCategoryId id);
}
