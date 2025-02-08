package br.studio.ticketmonster.category;

import java.time.LocalDateTime;

import br.studio.ticketmonster.infra.Default;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class EventCategory extends PanacheEntity {

    
    @Column(unique = true , nullable = false)    
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public EventCategory() {
    }

    @Default
    public EventCategory(String description, String name) {
        this.description = description;
        this.name = name;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(EventCategory input) {
        this.description = input.getDescription();
        this.name = input.getName();
        this.updatedAt = LocalDateTime.now();
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "EventCategory [description=" + description + ", name=" + name + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt
                + "]";
    }

    

    

}
