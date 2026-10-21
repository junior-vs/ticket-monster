/**
 * Camada de dominio do microsservico catalog-service.
 *
 * <p>Responsabilidade: excecoes de negocio do dominio do catalogo (categoria
 * duplicada, inexistente, em uso, invalida).
 *
 * <p>Regra de dependencia (Clean Architecture): nucleo puro; nao importa
 * Quarkus, JPA, Redis ou Kafka.
 */
package br.vsjr.labs.ticketmonster.catalog.domain.exception;
