/**
 * Camada de aplicacao do microsservico catalog-service.
 *
 * <p>Responsabilidade: implementacao dos casos de uso (um caso de uso por
 * operacao), orquestrando dominio e portas de saida.
 *
 * <p>Regra de dependencia (Clean Architecture): aponta para dentro; depende de
 * {@code domain} e de {@code application.port.out}, nunca de adaptadores.
 */
package br.vsjr.labs.ticketmonster.catalog.application.usecase;
