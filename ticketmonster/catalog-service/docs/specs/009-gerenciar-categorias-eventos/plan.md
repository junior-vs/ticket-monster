# Implementation Plan: Gerenciar Catálogo de Categorias de Eventos (US-CAT-09)

**Branch**: `009-gerenciar-categorias-eventos` | **Date**: 2026-07-26 | **Spec**: [specs/009-gerenciar-categorias-eventos/spec.md](file:///e:/develop/repos/java-projects/ticket-monster/specs/009-gerenciar-categorias-eventos/spec.md)

**Input**: Feature specification from `specs/009-gerenciar-categorias-eventos/spec.md`, `specs/microservice-catalog.spec.md`, and `docs/arch/arquitetura-solucao.md`.

## Summary

Implementar o CRUD completo da entidade raiz `EventCategory` no microsserviço `microservice-catalog` (Fase 0 da arquitetura de catálogo). A solução expõe endpoints REST para criação (POST), alteração (PUT), exclusão (DELETE) administrativa e listagem (GET) pública. Operações de escrita são protegidas via Keycloak JWT RBAC (`ROLE_ADMIN`), enquanto consultas utilizam estratégia Cache-Aside via Redis sob a chave `catalog:categories:list` (FR-007) para garantir latência P95 <= 50ms. O sistema assegura unicidade e normalização de descrição com `btrim()` no use case e constraint `CHECK (description = btrim(description))` no banco (RN06, FR-002a), impede a exclusão de categorias vinculadas a eventos (`ON DELETE RESTRICT` / RN04) e formata todas as exceções seguindo o padrão RFC 7807 (Problem Details).

## Technical Context

**Language/Version**: Java 21 (Quarkus 3.37.4+)

**Primary Dependencies**: 
- `quarkus-rest` + `quarkus-rest-jackson` (RESTEasy Reactive)
- `quarkus-hibernate-reactive-panache` + `quarkus-reactive-pg-client`
- `quarkus-redis-client` (Cache-Aside Reativo)
- `quarkus-oidc` (Keycloak JWT RBAC)
- `quarkus-smallrye-fault-tolerance` & `quarkus-smallrye-health`

**Storage**: PostgreSQL (`catalog_db`, schema `catalog`, tabela `event_category`) + Redis Cache (`catalog:categories:list`)

**Testing**: JUnit 5, RestAssured, Testcontainers (PostgreSQL, Redis), Quarkus Security Test (`@TestSecurity`)

**Target Platform**: Linux Container / GraalVM Native Image no Kubernetes

**Project Type**: Microservice (Web Service REST API)

**Performance Goals**: 
- Leitura pública via Redis Cache-Aside: latência P95 <= 50 ms (PR-002)
- Escritas de categoria (POST/PUT/DELETE): latência P95 <= 150 ms (SC-001 / PR-001)

**Constraints**: 
- 100% de bloqueio com HTTP 409 RFC 7807 em tentativas de exclusão de categoria com eventos vinculados (SC-001, RN04)
- Zero categorias duplicadas ou vazias no banco de dados, incluindo normalização por trim (RN06, FR-002a, SC-002)
- Taxa de erro não tratado (5xx) em steady-state < 0,1% (PR-001)

**Scale/Scope**: Entidade raiz do microsserviço `microservice-catalog`, sem dependências de FK de saída (RN06 / US-CAT-09).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Confirm layer boundaries**: PASS. O domínio (`EventCategory`) é puro e agnóstico de framework. Casos de uso (`application`) encapsulam as regras de negócio e normalização (trim) e interagem com portas (`adapter-out` Panache/Redis) e interfaces REST (`adapter-in`).
- **Confirm contract-first scope**: PASS. Contrato OpenAPI 3.0 especificado em `contracts/event-categories-api.yaml` cobrindo requisições, respostas e formato RFC 7807 Problem Details.
- **Confirm test depth**: PASS. Estratégia de testes estruturada cobrindo testes unitários de domínio (incluindo validação e trim de FR-002a), testes de contrato REST, testes de integração com Testcontainers (Postgres + Redis) e teste E2E do fluxo P1.
- **Confirm UX consistency**: PASS. Respostas de erro padronizadas em RFC 7807, semântica HTTP estrita (201, 200, 204, 400, 401, 403, 404, 409) e identificadores UUID v4.
- **Confirm performance budgets**: PASS. Metas de latência de escrita (<= 150ms P95) e leitura cacheada (<= 50ms P95) validadas.
- Exceptions: Nenhuma exceção necessária.

## Project Structure

### Documentation (this feature)

```text
specs/009-gerenciar-categorias-eventos/
├── spec.md              # Feature specification
├── plan.md              # Implementation plan (this file)
├── research.md          # Phase 0 output (architectural research & decisions)
├── data-model.md        # Phase 1 output (data model & schema DDL)
├── quickstart.md        # Phase 1 output (runnable validation guide)
└── contracts/           # Phase 1 output (OpenAPI contract)
    └── event-categories-api.yaml
```

### Source Code (`microservice-catalog`)

```text
microservice-catalog/src/
├── main/
│   ├── java/br/vsjr/labs/ticketmonster/catalog/
│   │   ├── domain/
│   │   │   ├── entity/EventCategory.java
│   │   │   ├── exception/CategoryAlreadyExistsException.java
│   │   │   ├── exception/CategoryNotFoundException.java
│   │   │   └── exception/CategoryInUseException.java
│   │   ├── application/
│   │   │   ├── port/in/CreateCategoryUseCase.java
│   │   │   ├── port/in/UpdateCategoryUseCase.java
│   │   │   ├── port/in/DeleteCategoryUseCase.java
│   │   │   ├── port/in/ListCategoriesUseCase.java
│   │   │   └── service/EventCategoryApplicationService.java
│   │   ├── adapter/
│   │   │   ├── in/rest/
│   │   │   │   ├── EventCategoryResource.java
│   │   │   │   ├── dto/CreateCategoryRequest.java
│   │   │   │   ├── dto/UpdateCategoryRequest.java
│   │   │   │   ├── dto/CategoryResponse.java
│   │   │   │   └── mapper/RFC7807ExceptionMapper.java
│   │   │   └── out/persistence/
│   │   │       ├── EventCategoryPanacheRepository.java
│   │   │       ├── EventCategoryEntity.java
│   │   │       └── RedisCategoryCacheAdapter.java
│   └── resources/
│       ├── application.properties
│       └── db/migration/V1.0.1__create_event_category_table.sql
└── test/
    └── java/br/vsjr/labs/ticketmonster/catalog/
        ├── unit/domain/EventCategoryTest.java
        ├── integration/EventCategoryRepositoryTest.java
        └── rest/EventCategoryResourceTest.java
```

**Structure Decision**: Selecionada estrutura de microsserviço Java com Quarkus 3.37+ no módulo `microservice-catalog`, seguindo Clean Architecture em pacotes por camada (`domain`, `application`, `adapter`).

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| *Nenhuma violação* | N/A | N/A |

## Planned Phases & Artifacts

- **Phase 0: Outline & Research** -> `research.md` (Atualizado)
- **Phase 1: Design & Contracts** -> `data-model.md`, `contracts/event-categories-api.yaml`, `quickstart.md` (Atualizado)
- **Phase 2: Task Generation** -> `tasks.md` (Será gerado pelo `/speckit-tasks`)
