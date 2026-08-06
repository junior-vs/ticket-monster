package br.vsjr.labs.ticketmoster.catalog.application.port.in;

import br.vsjr.labs.ticketmoster.catalog.domain.model.EventCategoryDescription;
import br.vsjr.labs.ticketmoster.catalog.domain.model.EventCategoryId;
import br.vsjr.labs.ticketmoster.catalog.domain.model.EventCategory;
import io.smallrye.mutiny.Uni;

public interface UpdateCategoryUseCase {

    public Uni<EventCategory> updateCategory(EventCategoryId id, EventCategoryDescription newDescription);
}
