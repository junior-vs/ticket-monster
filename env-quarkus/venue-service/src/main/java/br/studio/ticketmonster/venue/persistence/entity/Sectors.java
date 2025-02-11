package br.studio.ticketmonster.venue.persistence.entity;    

import jakarta.persistence.*;
import java.util.UUID;



@Entity
@Table(name = "sectors")
public class Sectors {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;


    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "layout")
    private String layout; // Assuming layout is stored as a JSON string

    @ManyToOne
    @JoinColumn(name = "venue_id", nullable = false)
    private Venues venues;

    

    public Sectors() {
    }

    public Sectors(String name, int capacity, String layout, Venues venues) {
        this.name = name;
        this.capacity = capacity;
        this.layout = layout;
        this.venues = venues;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getLayout() {
        return layout;
    }

    public Venues getVenue() {
        return venues;
    }

   
}