# Implementation Plan: US-CAT-10 - Gerenciar Venue e Section

**Branch**: `010-gerenciar-locais-secoes` | **Date**: 2026-07-29 | **Spec**: `microservice-catalog/specs/010-gerenciar-locais-secoes/spec.md`

**Input**: Feature specification from `microservice-catalog/specs/010-gerenciar-locais-secoes/spec.md`

## Summary

Implementar CRUD administrativo e leitura publica para `Venue` e `Section` no `microservice-catalog`, preservando RN07, RN11 e RN12 diretamente nas invariantes de dominio e nas constraints PostgreSQL existentes. `Section.capacity` permanece somente leitura e derivada pelo banco via coluna gerada `GENERATED ALWAYS AS (number_of_rows * row_capacity) STORED`; payloads de escrita nao devem aceitar ou propagar `capacity`.

A solucao usa Quarkus REST reativo, camada de aplicacao com casos de uso, dominio sem dependencias de framework, persistencia via Hibernate Reactive Panache/PostgreSQL, cache-aside Redis para leituras de Venue/Section e contratos REST versionados em `/api/v1`.

## Technical Context

**Language/Version**: Java 25 conforme `microservice-catalog/pom.xml` (`maven.compiler.release=25`). A constituicao define Java 21 como baseline minimo para novos servicos; Java 25 e uma versao acima do baseline e deve ser mantida apenas se o runtime alvo suportar esse release.

**Primary Dependencies**: Quarkus 3.37.4; `quarkus-rest`, `quarkus-rest-jackson`, `quarkus-hibernate-reactive-panache`, `quarkus-reactive-pg-client`, `quarkus-jdbc-postgresql`, `quarkus-liquibase`, `quarkus-redis-client`, `quarkus-messaging-kafka`, `quarkus-oidc`, `quarkus-opentelemetry`, `quarkus-micrometer-registry-prometheus`, `quarkus-smallrye-health`, `quarkus-smallrye-fault-tolerance`.

**Storage**: PostgreSQL `catalog_db`, schema `catalog`, com Liquibase aplicando `db/changelog/db.changelog-master.xml` -> `db/migration/V1__init.sql`; Redis DB 0 para cache-aside (`catalog:venue:{id}` e `catalog:venue:{id}:sections`); Kafka topic `catalog-events` ja configurado, mas eventos de delecao/alteracao de Section para inventario ainda nao tem contrato aprovado.

**Testing**: Maven com Quarkus JUnit e REST Assured. Plano de testes: unitarios de dominio/casos de uso, contratos REST, integracao com Testcontainers PostgreSQL/Redis/Kafka para fluxos afetados e E2E do fluxo P1 de criacao de Venue.

**Target Platform**: Microsservico backend Quarkus executando em container Linux/Kubernetes ou local via Docker Compose compartilhado.

**Project Type**: Web service REST reativo em arquitetura limpa/hexagonal dentro de `microservice-catalog`.

**Performance Goals**: Leituras publicas de catalogo p95 <= 250 ms em cache quente; escritas administrativas p95 <= 200 ms sob carga administrativa acordada; erro 5xx em steady-state < 0,1% para esta feature.

**Constraints**: Erros via RFC 7807; rotas administrativas exigem `ROLE_ADMIN`; paginacao publica/admin usa `page` e `size` base 0; dominio nao depende de HTTP, ORM, Kafka ou Redis; sem chamada sincronica para `microservice-inventory`; `capacity` nunca e entrada de escrita.

**Scale/Scope**: CRUD de `Venue` e `Section`, leitura de lista/detalhe de Venues e secoes, invalidacao sincrona de cache de Venue/Section, contratos REST v1 e hooks de observabilidade para latencia/erros.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- Layer boundaries: PASS. A implementacao deve separar `domain`, `application`, `adapter-in/rest` e `adapter-out` para Postgres/Redis/Kafka. Entidades de dominio e validacoes RN07/RN11/RN12 nao devem depender de Quarkus, JPA/Panache ou annotations HTTP.
- Contract-first scope: PASS WITH RISK. Contrato REST v1 definido em `contracts/openapi.yaml`. Eventos `SectionDeleted`, `SectionUpdated` com capacidade alterada e `VenueDeleted` sao dependencia arquitetural ainda nao aprovada; a implementacao desta feature nao deve publicar evento novo sem ADR/contrato complementar.
- Test depth: PASS. Unitarios, contrato REST, integracao Testcontainers e E2E P1 estao previstos em `quickstart.md`.
- UX consistency: PASS. RFC 7807, 401/403 para auth, 404 para ausentes, 409 para unicidade/restricao relacional, paginacao base 0 e UUIDs em todos os recursos.
- Performance budgets: PASS. Orçamentos e validacao por metricas Prometheus, testes de integracao e smoke/local estao definidos.

Post-design re-check: PASS WITH TRACKED RISK. Os artefatos de fase 1 mantem o gap de eventos para inventario como risco explicito e nao introduzem acoplamento sincrono cross-service.

## Project Structure

### Documentation (this feature)

```text
microservice-catalog/specs/010-gerenciar-locais-secoes/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── openapi.yaml
└── tasks.md
```

### Source Code (repository root)

```text
microservice-catalog/
├── pom.xml
├── src/main/java/br/vsjr/labs/ticketmoster/
│   ├── catalog/venue/domain/
│   ├── catalog/venue/application/
│   ├── catalog/venue/adapter/in/rest/
│   └── catalog/venue/adapter/out/
│       ├── persistence/
│       ├── cache/
│       └── messaging/
├── src/main/resources/
│   ├── application.properties
│   └── db/
│       ├── changelog/db.changelog-master.xml
│       └── migration/V1__init.sql
└── src/test/java/br/vsjr/labs/ticketmoster/
    ├── catalog/venue/domain/
    ├── catalog/venue/application/
    ├── catalog/venue/adapter/in/rest/
    └── catalog/venue/adapter/out/
```

**Structure Decision**: Usar modulo unico Quarkus do `microservice-catalog` com pacotes por feature/agregado e fronteiras logicas de clean architecture. O projeto atual ainda contem apenas o `GreetingResource`; a feature deve introduzir os pacotes acima sem criar novo modulo Maven.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Event propagation gap for inventory | Inventory snapshots dependem de eventos de catalogo, mas `SectionDeleted`/`VenueDeleted` e semantica de reducao de capacidade nao estao especificados | Publicar eventos ad hoc violaria disciplina contract-first e poderia quebrar consumidores |
