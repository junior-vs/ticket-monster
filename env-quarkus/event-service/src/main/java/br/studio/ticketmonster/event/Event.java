package br.studio.ticketmonster.event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import br.studio.ticketmonster.category.EventCategory;
import br.studio.ticketmonster.infra.Default;
import br.studio.ticketmonster.mediaitem.MediaItem;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;

@Entity

// search event by id and bring event with media items and category
@NamedQuery(name = "Event.findByIdWithMediaItems", query = "SELECT e FROM Event e LEFT JOIN FETCH e.mediaItems JOIN e.category  WHERE e.id = :id")
public class Event extends PanacheEntity {

    private String name;
    private String description;
    
    private LocalDate startDate;
    private LocalDate endDate;
    private String location;

    private UUID uuid;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private EventCategory category;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MediaItem> mediaItems;

    public Event() {
    }

    @Default
    public Event(String name, String description, LocalDate startDate, LocalDate endDate, String location, EventCategory category) {
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.location = location;
        this.category = category;        
        this.uuid = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(Event input) {
        this.name = input.getName();
        this.description = input.getDescription();
        this.startDate = input.getStartDate();
        this.endDate = input.getEndDate();
        this.location = input.getLocation();
        this.category = input.getCategory();
        this.updatedAt = LocalDateTime.now();
    }

    public void addMediaItem(MediaItem mediaItem) {
        mediaItems.add(mediaItem);
        mediaItem.setEvent(this);
    }

    

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getLocation() {
        return location;
    }

    public UUID getUuid() {
        return uuid;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public EventCategory getCategory() {
        return category;
    }

    public List<MediaItem> getMediaItems() {
        return mediaItems;
    }



    @Override
    public String toString() {
        return "Event [name=" + name + ", description=" + description + ", startDate=" + startDate + ", endDate="
                + endDate + ", location=" + location + ", uuid=" + uuid + ", createdAt=" + createdAt + ", updatedAt="
                + updatedAt + ", category=" + category + ", mediaItems=" + mediaItems + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Event other = (Event) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }

}
