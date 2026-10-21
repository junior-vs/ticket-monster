/**
 * Camada de entrada (adapter-in) do microsservico catalog-service.
 *
 * <p>Responsabilidade: expor os endpoints REST (JAX-RS / RESTEasy Reactive) do
 * catalogo de categorias de eventos.
 *
 * <p>Regra de dependencia (Clean Architecture): aponta para dentro, acionando os
 * casos de uso da camada {@code application}; nao contem regra de negocio.
 */
package br.vsjr.labs.ticketmonster.catalog.adapter.in.rest;
