package com.ticket.monster.event.domain.model;

import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;


@Entity
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "show_seq")
    @SequenceGenerator(name = "show_seq", sequenceName = "show_seq", allocationSize = 1, initialValue = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "show", cascade = CascadeType.ALL)
    @OrderBy("date")
    private Set<Performance> performances;

    public Show() {
    }

    private Show(Builder builder) {
        this.id = builder.id;
        this.event = builder.event;
        this.venue = builder.venue;
        this.performances = builder.performances;
    }

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public Venue getVenue() {
        return venue;
    }

    public Set<Performance> getPerformances() {
        return performances;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private Event event;
        private Venue venue;
        private Set<Performance> performances;

        private Builder() {}

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder event(Event event) {
            this.event = event;
            return this;
        }

        public Builder venue(Venue venue) {
            this.venue = venue;
            return this;
        }

        public Builder performances(Set<Performance> performances) {
            this.performances = performances;
            return this;
        }

        public Show build() {
            return new Show(this);
        }
    }

}
