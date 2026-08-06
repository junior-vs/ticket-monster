package br.vsjr.labs.ticketmonster.catalog.application.port.in;

import br.vsjr.labs.ticketmonster.catalog.domain.model.EventCategory;
import br.vsjr.labs.ticketmonster.catalog.domain.vo.PageRequest;
import io.smallrye.mutiny.Uni;

import java.util.List;

public interface ListCategoriesUseCase {

    public Uni<List<EventCategory>>  listAllCategories(PageRequest pageRequest);
}
