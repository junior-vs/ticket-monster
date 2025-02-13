package br.studio.ticketmonster.venue.model;

import java.util.UUID;

public record VenuesRequest (UUID id, String name, String description) {

}
