package br.studio.ticketmonster.venue.model;

import java.util.UUID;

public record VenuesListResponse (UUID id, String name, String description) {

}
