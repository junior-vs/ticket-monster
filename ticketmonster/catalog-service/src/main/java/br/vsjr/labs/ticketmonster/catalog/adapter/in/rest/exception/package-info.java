/**
 * Camada de entrada (adapter-in) do microsservico catalog-service.
 *
 * <p>Responsabilidade: tratamento de excecoes da API e serializacao de erros no
 * padrao RFC 7807 (Problem Details) via ExceptionMapper JAX-RS.
 *
 * <p>Regra de dependencia (Clean Architecture): aponta para dentro; nao contem
 * regra de negocio.
 */
package br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.exception;
