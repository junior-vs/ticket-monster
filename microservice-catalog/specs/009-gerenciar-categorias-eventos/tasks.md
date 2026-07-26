# Tasks: Gerenciar Catálogo de Categorias de Eventos (US-CAT-09)

**Input**: Design documents from `specs/009-gerenciar-categorias-eventos/`
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/event-categories-api.yaml`, `quickstart.md`
**Constitution Compliance**: Required tests for domain rules, contract validation, Testcontainers integration, and P1 E2E flow.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: User story label (e.g., `[US1]`, `[US2]`, `[US3]`, `[US4]`)
- File paths are relative to repository root (`microservice-catalog/...`)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize project structure and baseline configuration for `microservice-catalog`

- [ ] T001 Configure application properties for Quarkus OIDC, PostgreSQL, and Redis in `microservice-catalog/src/main/resources/application.properties`
- [ ] T002 [P] Create package layout for Clean Architecture in `microservice-catalog/src/main/java/br/vsjr/labs/ticketmonster/catalog/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core database schema, domain model, and exception mapping that all user stories depend on

**⚠️ CRITICAL**: No user story implementation can begin until this phase is complete

- [ ] T003 Create Flyway migration script for `catalog.event_category` table with `UNIQUE` and `btrim()` constraints in `microservice-catalog/src/main/resources/db/migration/V1.0.1__create_event_category_table.sql`
- [ ] T004 [P] Create domain entity `EventCategory` with validation and `trim()` normalization in `microservice-catalog/src/main/java/br/vsjr/labs/ticketmonster/catalog/domain/entity/EventCategory.java`
- [ ] T005 [P] Create domain exceptions (`CategoryAlreadyExistsException`, `CategoryNotFoundException`, `CategoryInUseException`) in `microservice-catalog/src/main/java/br/vsjr/labs/ticketmonster/catalog/domain/exception/`
- [ ] T006 [P] Create JPA entity `EventCategoryEntity` in `microservice-catalog/src/main/java/br/vsjr/labs/ticketmonster/catalog/adapter/out/persistence/EventCategoryEntity.java`
- [ ] T007 Create Panache reactive repository `EventCategoryPanacheRepository` in `microservice-catalog/src/main/java/br/vsjr/labs/ticketmonster/catalog/adapter/out/persistence/EventCategoryPanacheRepository.java`
- [ ] T008 [P] Create Redis cache adapter `RedisCategoryCacheAdapter` for invalidation and retrieval under key `catalog:categories:list` in `microservice-catalog/src/main/java/br/vsjr/labs/ticketmonster/catalog/adapter/out/persistence/RedisCategoryCacheAdapter.java`
- [ ] T009 Create RFC 7807 Problem Details exception mapper `RFC7807ExceptionMapper` in `microservice-catalog/src/main/java/br/vsjr/labs/ticketmonster/catalog/adapter/in/rest/mapper/RFC7807ExceptionMapper.java`

**Checkpoint**: Foundation ready — database schema, domain invariants, repository ports, and RFC 7807 error handler complete.

---

## Phase 3: User Story 1 - Cadastrar Nova Categoria de Evento (Priority: P1) 🎯 MVP

**Goal**: Permitir que administradores cadastrem novas categorias de evento com descrição única e recebam status 201 Created (ou 409 Conflict se duplicado / 400 Bad Request se inválido).

**Independent Test**: Enviar requisição HTTP POST para `/api/v1/event-categories` com token `ROLE_ADMIN` e verificar geração de UUID v4 e HTTP 201.

### Tests for User Story 1 (REQUIRED) ⚠️

- [ ] T010 [P] [US1] Unit test for `EventCategory` validation and `trim()` normalization in `microservice-catalog/src/test/java/br/vsjr/labs/ticketmonster/catalog/unit/domain/EventCategoryTest.java`
- [ ] T011 [P] [US1] Integration and E2E test for category creation, uniqueness check (RN06, FR-002a), and RFC 7807 error responses in `microservice-catalog/src/test/java/br/vsjr/labs/ticketmonster/catalog/rest/EventCategoryResourceTest.java`

### Implementation for User Story 1

- [ ] T012 [P] [US1] Create input DTO `CreateCategoryRequest` and output DTO `CategoryResponse` in `microservice-catalog/src/main/java/br/vsjr/labs/ticketmonster/catalog/adapter/in/rest/dto/`
- [ ] T013 [P] [US1] Create use case interface `CreateCategoryUseCase` in `microservice-catalog/src/main/java/br/vsjr/labs/ticketmonster/catalog/application/port/in/CreateCategoryUseCase.java`
- [ ] T014 [US1] Implement `CreateCategoryUseCase` logic with uniqueness check and cache eviction in `microservice-catalog/src/main/java/br/vsjr/labs/ticketmonster/catalog/application/service/EventCategoryApplicationService.java`
- [ ] T015 [US1] Implement POST endpoint `/api/v1/event-categories` guarded with `@RolesAllowed("ROLE_ADMIN")` in `microservice-catalog/src/main/java/br/vsjr/labs/ticketmonster/catalog/adapter/in/rest/EventCategoryResource.java`

**Checkpoint**: At this point, User Story 1 (MVP) is fully functional and testable independently.

---

## Phase 4: User Story 2 - Alterar Descrição de Categoria Existente (Priority: P2)

**Goal**: Permitir que administradores alterem a descrição de uma categoria existente com validação de unicidade.

**Independent Test**: Enviar requisição HTTP PUT para `/api/v1/event-categories/{id}` com a nova descrição e verificar retorno HTTP 200 (ou 409 Conflict se duplicado / 404 se ID inexistente).

### Tests for User Story 2 (REQUIRED) ⚠️

- [ ] T016 [P] [US2] Contract and REST integration test for update category flow in `microservice-catalog/src/test/java/br/vsjr/labs/ticketmonster/catalog/rest/EventCategoryResourceTest.java`

### Implementation for User Story 2

- [ ] T017 [P] [US2] Create input DTO `UpdateCategoryRequest` in `microservice-catalog/src/main/java/br/vsjr/labs/ticketmonster/catalog/adapter/in/rest/dto/UpdateCategoryRequest.java`
- [ ] T018 [P] [US2] Create use case interface `UpdateCategoryUseCase` in `microservice-catalog/src/main/java/br/vsjr/labs/ticketmonster/catalog/application/port/in/UpdateCategoryUseCase.java`
- [ ] T019 [US2] Implement `UpdateCategoryUseCase` logic with uniqueness validation and cache invalidation in `microservice-catalog/src/main/java/br/vsjr/labs/ticketmonster/catalog/application/service/EventCategoryApplicationService.java`
- [ ] T020 [US2] Implement PUT endpoint `/api/v1/event-categories/{id}` guarded with `@RolesAllowed("ROLE_ADMIN")` in `microservice-catalog/src/main/java/br/vsjr/labs/ticketmonster/catalog/adapter/in/rest/EventCategoryResource.java`

**Checkpoint**: User Stories 1 AND 2 are both functional and testable independently.

---

## Phase 5: User Story 3 - Excluir Categoria Sem Eventos Associados (Priority: P3)

**Goal**: Permitir a exclusão de categorias não utilizadas e proibir (`ON DELETE RESTRICT` / RN04) a remoção de categorias associadas a eventos.

**Independent Test**: Enviar requisição HTTP DELETE para uma categoria sem eventos e verificar HTTP 204 No Content. Enviar DELETE para uma categoria com eventos vinculados e verificar HTTP 409 Conflict (RFC 7807).

### Tests for User Story 3 (REQUIRED) ⚠️

- [ ] T021 [P] [US3] Integration test for category deletion and `ON DELETE RESTRICT` constraint validation in `microservice-catalog/src/test/java/br/vsjr/labs/ticketmonster/catalog/integration/EventCategoryRepositoryTest.java`
- [ ] T022 [P] [US3] REST integration test for DELETE endpoint status codes (204, 404, 409, 401, 403) in `microservice-catalog/src/test/java/br/vsjr/labs/ticketmonster/catalog/rest/EventCategoryResourceTest.java`

### Implementation for User Story 3

- [ ] T023 [P] [US3] Create use case interface `DeleteCategoryUseCase` in `microservice-catalog/src/main/java/br/vsjr/labs/ticketmonster/catalog/application/port/in/DeleteCategoryUseCase.java`
- [ ] T024 [US3] Implement `DeleteCategoryUseCase` logic checking for associated events, throwing `CategoryInUseException`, and invalidating Redis cache in `microservice-catalog/src/main/java/br/vsjr/labs/ticketmonster/catalog/application/service/EventCategoryApplicationService.java`
- [ ] T025 [US3] Implement DELETE endpoint `/api/v1/event-categories/{id}` guarded with `@RolesAllowed("ROLE_ADMIN")` in `microservice-catalog/src/main/java/br/vsjr/labs/ticketmonster/catalog/adapter/in/rest/EventCategoryResource.java`

**Checkpoint**: User Stories 1, 2, and 3 are all functional and testable independently.

---

## Phase 6: User Story 4 - Listar e Consultar Categorias de Eventos (Priority: P4)

**Goal**: Fornecer a listagem pública de todas as categorias cadastradas, ordenadas por descrição ASC, com otimização Cache-Aside via Redis.

**Independent Test**: Efeticar requisição HTTP GET sem token em `/api/v1/event-categories` e verificar lista ordenada com status HTTP 200 e resposta do Redis nas requisições subsequentes em <= 50 ms.

### Tests for User Story 4 (REQUIRED) ⚠️

- [ ] T026 [P] [US4] Contract and Redis cache-aside test for public GET endpoint in `microservice-catalog/src/test/java/br/vsjr/labs/ticketmonster/catalog/rest/EventCategoryResourceTest.java`

### Implementation for User Story 4

- [ ] T027 [P] [US4] Create use case interface `ListCategoriesUseCase` in `microservice-catalog/src/main/java/br/vsjr/labs/ticketmonster/catalog/application/port/in/ListCategoriesUseCase.java`
- [ ] T028 [US4] Implement `ListCategoriesUseCase` logic with Redis cache-aside lookup under key `catalog:categories:list` in `microservice-catalog/src/main/java/br/vsjr/labs/ticketmonster/catalog/application/service/EventCategoryApplicationService.java`
- [ ] T029 [US4] Implement public GET endpoint `/api/v1/event-categories` in `microservice-catalog/src/main/java/br/vsjr/labs/ticketmonster/catalog/adapter/in/rest/EventCategoryResource.java`

**Checkpoint**: All user stories (P1 through P4) are fully functional.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Quality gates, OpenAPI documentation, test suite verification, and validation against performance budgets

- [ ] T030 [P] Verify OpenAPI 3.0 contract alignment against `specs/009-gerenciar-categorias-eventos/contracts/event-categories-api.yaml`
- [ ] T031 Execute quickstart validation scenarios defined in `specs/009-gerenciar-categorias-eventos/quickstart.md`
- [ ] T032 Run full Maven test suite (`./mvnw clean test`) and confirm 100% test pass rate in `microservice-catalog/`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Can start immediately.
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories.
- **User Stories (Phase 3+)**: Depend on Foundational phase completion. Proceed sequentially by priority (P1 → P2 → P3 → P4) or in parallel per developer.
- **Polish (Phase 7)**: Depends on all user stories being complete.

### User Story Execution Graph

```
[Phase 1: Setup] ──> [Phase 2: Foundational]
                             │
     ┌───────────────────────┼───────────────────────┬───────────────────────┐
     ▼                       ▼                       ▼                       ▼
[Phase 3: US1 (P1)]     [Phase 4: US2 (P2)]     [Phase 5: US3 (P3)]     [Phase 6: US4 (P4)]
     │                       │                       │                       │
     └───────────────────────┴───────────────────────┴───────────────────────┘
                                             │
                                             ▼
                                 [Phase 7: Polish & Gates]
```

### Parallel Opportunities per Phase

- **Foundational Phase**: T004, T005, T006, T008 can run concurrently.
- **User Story 1**: T010, T011, T012, T013 can run concurrently before service integration.
- **User Story 2**: T016, T017, T018 can run concurrently.
- **User Story 3**: T021, T022, T023 can run concurrently.
- **User Story 4**: T026, T027 can run concurrently.

---

## Implementation Strategy

### MVP First Scope (User Story 1)
1. Complete Phase 1 (Setup) and Phase 2 (Foundational).
2. Implement Phase 3 (User Story 1 - Create Category).
3. Validate User Story 1 independently with unit & integration tests.

### Incremental Rollout
- Increment 1: MVP (Creation of categories) -> Deployable
- Increment 2: Category Editing (US2) -> Deployable
- Increment 3: Category Deletion with `ON DELETE RESTRICT` (US3) -> Deployable
- Increment 4: Cached Public Listing (US4) -> Production Ready
