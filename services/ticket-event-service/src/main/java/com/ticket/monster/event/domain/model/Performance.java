package com.ticket.monster.event.domain.model;

import java.time.ZonedDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "performance", uniqueConstraints = @UniqueConstraint(columnNames = {"show_id", "date"}))
public class Performance {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "performance_seq")
    @SequenceGenerator(name = "performance_seq", sequenceName = "performance_seq", allocationSize = 1, initialValue = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private Appearance show;

    @Column(name = "date", nullable = false)
    private ZonedDateTime date;

    public Performance() {
    }

    public Performance(Appearance show, ZonedDateTime date) {
        this.show = Objects.requireNonNull(show);
        this.date = Objects.requireNonNull(date);
    }

    public Long getId() {
        return id;
    }

    public Appearance getShow() {
        return show;
    }

    public ZonedDateTime getDate() {
        return date;
    }

    public static Performance of(Appearance show, ZonedDateTime date) {
        return new Performance(show, date);
    }
}