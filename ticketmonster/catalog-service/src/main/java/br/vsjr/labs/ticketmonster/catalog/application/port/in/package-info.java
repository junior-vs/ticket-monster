/**
 * Camada de aplicacao do microsservico catalog-service.
 *
 * <p>Responsabilidade: portas de entrada (interfaces de casos de uso) expostas
 * pela aplicacao para os adaptadores de entrada.
 *
 * <p>Regra de dependencia (Clean Architecture): aponta para dentro; nada de
 * detalhes de framework, persistencia ou transporte.
 */
package br.vsjr.labs.ticketmonster.catalog.application.port.in;
