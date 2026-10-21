/**
 * Camada de dominio do microsservico catalog-service.
 *
 * <p>Responsabilidade: eventos de dominio publicados pelo agregado de catalogo.
 *
 * <p>Regra de dependencia (Clean Architecture): nucleo puro; nao importa
 * Quarkus, JPA, Redis ou Kafka.
 */
package br.vsjr.labs.ticketmonster.catalog.domain.event;
