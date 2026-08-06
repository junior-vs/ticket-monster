package br.vsjr.labs.ticketmoster.catalog.application.port.in;

import br.vsjr.labs.ticketmoster.catalog.domain.model.EventCategory;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Valid;

public interface CreateCategoryUseCase {

    public Uni<EventCategory> createCategory(@Valid EventCategory category);


}
