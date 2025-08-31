package com.ticket.monster.event.domain.model;

import java.time.ZonedDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;

@Entity
public class Performance {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "performance_seq", sequenceName = "performance_seq", allocationSize = 1, initialValue = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @Column(name = "date", nullable = false)
    private ZonedDateTime date;

    public Performance() {
    }

    private Performance(Builder builder) {
        this.id = builder.id;
        this.show = builder.show;
        this.date = builder.date;
    }

    public Long getId() {
        return id;
    }

    public Show getShow() {
        return show;
    }

    public ZonedDateTime getDate() {
        return date;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private Show show;
        private ZonedDateTime date;

        private Builder() {}

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder show(Show show) {
            this.show = show;
            return this;
        }

        public Builder date(ZonedDateTime date) {
            this.date = date;
            return this;
        }

        public Performance build() {
            return new Performance(this);
        }
    }
}
