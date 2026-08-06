package br.vsjr.labs.ticketmonster.catalog.application.port.in;

import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategory;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Valid;

public interface CreateCategoryUseCase {

    public Uni<EventCategory> createCategory(@Valid EventCategory category);


}
