package br.vsjr.labs.ticketmoster.catalog.application.port.in;

import br.vsjr.labs.ticketmoster.catalog.domain.model.EventCategory;
import br.vsjr.labs.ticketmoster.catalog.domain.vo.PageRequest;
import br.vsjr.labs.ticketmoster.catalog.domain.vo.PageResult;
import io.smallrye.mutiny.Uni;

import java.util.List;

public interface ListCategoriesUseCase {

    public Uni<List<EventCategory>>  listAllCategories(PageRequest pageRequest);
}
