/**
 * Camada de dominio do microsservico catalog-service.
 *
 * <p>Responsabilidade: modelo de dominio (agregados e entidades) da entidade
 * raiz {@code EventCategory}, rico em comportamento e invariantes.
 *
 * <p>Regra de dependencia (Clean Architecture): nucleo puro; nao importa
 * Quarkus, JPA, Redis ou Kafka.
 */
package br.vsjr.labs.ticketmonster.catalog.domain.model;
