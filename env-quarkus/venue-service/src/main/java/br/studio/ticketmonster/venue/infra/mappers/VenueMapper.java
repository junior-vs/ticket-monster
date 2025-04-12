package br.studio.ticketmonster.venue.infra.mappers;

import java.util.List;

import org.mapstruct.Mapper;

import br.studio.ticketmonster.venue.model.VenuesListResponse;
import br.studio.ticketmonster.venue.persistence.entity.Venues;

@Mapper
public interface VenueMapper {


    List<VenuesListResponse> toListResponse(List<Venues> venues);

}
