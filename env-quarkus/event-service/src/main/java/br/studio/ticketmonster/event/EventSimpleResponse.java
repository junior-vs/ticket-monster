package br.studio.ticketmonster.event;

import java.time.LocalDate;

public record EventSimpleResponse(
                Long id,
                String name,
                String description,
                LocalDate startDate,
                LocalDate endDate,
                String location,
                String category) {

}
