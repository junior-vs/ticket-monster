package br.studio.ticketmonster.category;

import java.time.LocalDateTime;

import br.studio.ticketmonster.infra.Default;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotEmpty;

@Entity
public class EventCategory extends PanacheEntity {

    @Column(unique = true , nullable = false)    
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public EventCategory() {
    }

    @Default
    public EventCategory(String description) {
        this.description = description;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(EventCategory input) {
        this.description = input.getDescription();
        this.updatedAt = LocalDateTime.now();
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "EventCategory [description=" + description + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt
                + "]";
    }

    

    

}
