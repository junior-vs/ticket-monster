package br.studio.ticketmonster.event;

import java.time.LocalDate;
import java.util.List;

import br.studio.ticketmonster.category.EventCategory;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Repository class for managing Event entities.
 * Provides methods to perform CRUD operations and custom queries on Event entities.
 * This class uses PanacheRepository for simplifying data access.
 */
@ApplicationScoped
public class EventRepository implements PanacheRepository<Event> {

    /**
     * Finds an Event by its ID along with its associated media items.
     *
     * @param id the ID of the Event to find
     * @return a Uni containing the Event with its media items, or null if not found
     */
    public Uni<Event> findByIdWithMediaItems(Long id) {
        return find("#Event.findByIdWithMediaItems", Parameters.with("id", id)).firstResult();
    }

    /**
     * Lists Events by their category with pagination support.
     *
     * @param category the ID of the category to filter Events by
     * @param pageIndex the index of the page to retrieve
     * @param pageSize the number of Events per page
     * @return a Uni containing a list of Events in the specified category and page
     */
    public Uni<List<Event>> searchEventsByCategory(EventCategory category, int pageIndex, int pageSize) {
        return find("category", category)
                .page(pageIndex, pageSize).list();
    }


    /**
     * Searches Events by their name with pagination support.
     *
     * @param startDate the start date to filter Events by
     * @param endDate the end date to filter Events by
     * @param pageIndex the index of the page to retrieve
     * @param pageSize the number of Events per page
     * @return a Uni containing a list of Events with the specified name and page
     */
    public Uni<List<Event>> searchEventsByDate(LocalDate startDate, LocalDate endDate, int pageIndex, int pageSize) {
        return find("eventDate between ?1 and ?2", startDate, endDate)
                .page(pageIndex, pageSize)
                .list();
    }



}
