package br.studio.ticketmonster.event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import br.studio.ticketmonster.infra.Default;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class Event extends PanacheEntity {

     

    private String name;
    private String description;
    private String category;
    private LocalDate startDate;
    private LocalDate endDate;
    private String location;
    private String imageUrl;
    private UUID uuid;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Event() {
    }

    @Default
    public Event(String name, String description, String category, LocalDate startDate, LocalDate endDate,
            String location,
            String imageUrl) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.startDate = startDate;
        this.endDate = endDate;
        this.location = location;
        this.imageUrl = imageUrl;
        this.uuid = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void update(Event event) {
        this.name = event.name;
        this.description = event.description;
        this.category = event.category;
        this.startDate = event.startDate;
        this.endDate = event.endDate;
        this.location = event.location;
        this.imageUrl = event.imageUrl;
        this.updatedAt = LocalDateTime.now();
    }

 
    @Override
    public String toString() {
        return "Event [name=" + name + ", description=" + description + ", category=" + category + ", startDate="
                + startDate + ", endDate=" + endDate + ", location=" + location + ", imageUrl=" + imageUrl + "]";
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
