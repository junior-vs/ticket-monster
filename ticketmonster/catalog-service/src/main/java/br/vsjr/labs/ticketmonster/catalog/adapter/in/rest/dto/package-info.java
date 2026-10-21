/**
 * Camada de entrada (adapter-in) do microsservico catalog-service.
 *
 * <p>Responsabilidade: Data Transfer Objects (requests/responses) da API REST,
 * desacoplados do modelo de dominio.
 *
 * <p>Regra de dependencia (Clean Architecture): aponta para dentro; nao contem
 * regra de negocio.
 */
package br.vsjr.labs.ticketmonster.catalog.adapter.in.rest.dto;
