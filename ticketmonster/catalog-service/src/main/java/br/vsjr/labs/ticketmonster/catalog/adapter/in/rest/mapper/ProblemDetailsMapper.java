package br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.dto.ProblemDetails;

/**
 * Mapeamento MapStruct (ADR 09) para o DTO de erro RFC 7807 {@link ProblemDetails}.
 *
 * <p>Centraliza a montagem do corpo {@code application/problem+json} usada pelos
 * mapeadores de excecao da borda REST, evitando a repeticao de {@code new
 * ProblemDetails(...)} em cada {@code @ServerExceptionMapper}.
 *
 * <p>A montagem usa multiplas fontes escalares; os nomes dos parametros precisam
 * ser preservados no bytecode ({@code -parameters}), o que ja ocorre no
 * {@code maven-compiler-plugin}.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface ProblemDetailsMapper {

    @Mapping(source = "type", target = "type")
    @Mapping(source = "title", target = "title")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "detail", target = "detail")
    @Mapping(source = "instance", target = "instance")
    ProblemDetails toProblemDetails(String type, String title, int status, String detail, String instance);
}
