# Tasks: Gerenciar Locais Físicos e Seções Estruturais (Admin)

**Input**: Design documents from `microservice-catalog/specs/010-gerenciar-locais-secoes/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), data-model.md

**Tests**: Testes são OBRIGATÓRIOS por Constituição III (E2E P1) e por FR-009 (unitários de domínio, contrato REST, integração Testcontainers Postgres/Redis, E2E P1).

**Organization**: Tasks agrupadas por user story, para implementação e teste independentes.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependência)
- **[Story]**: US1–US5
- Caminhos de arquivo relativos a `microservice-catalog/src/...`, pacote raiz `br.vsjr.labs.ticketmonster.catalog`

---

## Phase 0: Pré-condição de Ambiente (bloqueante, fora do escopo funcional desta feature)

> Estas tasks não implementam a feature de Venue/Section — resolvem um gap de build pré-existente da feature `event-categories` (009), que compartilha tipos com esta feature. Sem T000, `VenueExceptionMapper` não tem `ProblemDetails` para reutilizar.

- [ ] T000 [P] Criar `adapter/out/dto/ProblemDetails.java` (record RFC 7807: `type`, `title`, `status`, `detail`, `instance`) — hoje importado por `EventCategoryExceptionMapper` mas inexistente no repositório, quebrando o build do módulo. Criar como tipo compartilhado, sem acoplar a Venue/Section.
- [ ] T000a Registrar no PR desta feature um apontamento separado para o time responsável por `event-categories`: `EventCategoryRepositoryPort` e `EventCategoryCachePort` são referenciados por `EventCategoryService` mas não existem em `application/port/out` — o módulo não compila independentemente desta feature. Não é escopo de correção aqui.

---

## Phase 1: Setup

- [ ] T001 [P] Adicionar propriedade de cache em `src/main/resources/application.properties`, seguindo o padrão já usado para `catalog.cache.event-ttl`: `catalog.cache.venue-ttl=${CATALOG_CACHE_VENUE_TTL:PT1H}`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Domínio puro (VOs + records), exceções, portas de saída. `VenueEntity`/`SectionEntity`/`VenueAddressEmbeddable` **já existem** — não recriar.

**⚠️ CRITICAL**: Nenhuma US pode começar sem esta fase concluída.

- [ ] T002 [P] Criar VO `domain/model/VenueId.java` (record `UUID value`, `Objects.requireNonNull`, `static generate()`, `static from(String)`) — mesmo padrão de `EventCategoryId`.
- [ ] T003 [P] Criar VO `domain/model/VenueName.java` (record `String value`, compact constructor: trim, rejeita blank/null, `IllegalArgumentException` — RN07) — mesmo padrão de `EventCategoryDescription`.
- [ ] T004 [P] Criar VO `domain/model/Address.java` (record puro: `addressLine`, `city`, `state`, `postalCode`, `country`, todos nullable, sem validação de formato) — espelha `VenueAddressEmbeddable` mas sem anotação JPA.
- [ ] T005 [P] Criar record de domínio `domain/model/Venue.java` (`VenueId id`, `VenueName name`, `String description`, `Address address`, `Instant createdAt`), com construtor de conveniência a partir de `VenueEntity` e método `rename(VenueName newName)` retornando nova instância imutável (mesmo padrão `updateEventCategory` de `EventCategory`).
- [ ] T006 [P] Criar VO `domain/model/SectionId.java` (mesmo padrão de `VenueId`).
- [ ] T007 [P] Criar VO `domain/model/SectionName.java` (mesmo padrão de `VenueName`, RN11 — unicidade é responsabilidade do repositório, não da VO).
- [ ] T008 [P] Criar record de domínio `domain/model/Section.java` (`SectionId id`, `VenueId venueId`, `SectionName name`, `int numberOfRows`, `int rowCapacity`, `Integer capacity` — nullable, populado apenas ao reconstruir a partir de `SectionEntity` pós-persistência; compact constructor valida `numberOfRows > 0` e `rowCapacity > 0`, lança `IllegalArgumentException` — RN12/FR-004). `capacity` NUNCA é parâmetro aceito em fábrica de criação (`Section.forCreation(...)` sem capacity) — apenas no construtor de reconstrução a partir de entidade.
- [ ] T009 [P] Criar exceções em `domain/exception/`: `VenueAlreadyExistsException`, `VenueNotFoundException`, `VenueHasShowsException`, `InvalidVenueInputException`, `SectionAlreadyExistsException`, `SectionNotFoundException`, `InvalidSectionInputException` — mesmo padrão de `EventCategoryAlreadyExistsException` etc. (mensagem construída no construtor, `RuntimeException`).
- [ ] T010 [P] Criar porta `application/port/out/VenueRepositoryPort.java`: `Uni<Venue> save(Venue v)`, `Uni<Venue> update(Venue v)`, `Uni<Optional<Venue>> findById(VenueId id)`, `Uni<Boolean> existsByName(VenueName name, VenueId excludeIdIfAny)`, `Uni<PageResult<Venue>> findAll(PageRequest pageRequest)`, `Uni<Boolean> hasShows(VenueId id)`, `Uni<Void> deleteById(VenueId id)`.
- [ ] T011 [P] Criar porta `application/port/out/SectionRepositoryPort.java`: `Uni<Section> save(Section s)`, `Uni<Section> update(Section s)`, `Uni<Optional<Section>> findById(SectionId id)`, `Uni<List<Section>> findByVenueId(VenueId venueId)`, `Uni<Boolean> existsByNameInVenue(SectionName name, VenueId venueId, SectionId excludeIdIfAny)`, `Uni<Void> deleteById(SectionId id)`.
- [ ] T012 [P] Criar porta `application/port/out/VenueCachePort.java`: `Uni<Optional<Venue>> getVenue(VenueId id)`, `Uni<Void> setVenue(Venue v)`, `Uni<Void> evictVenue(VenueId id)`, `Uni<Optional<List<Section>>> getSections(VenueId venueId)`, `Uni<Void> setSections(VenueId venueId, List<Section> sections)`, `Uni<Void> evictSections(VenueId venueId)` — chaves `{prefix}:venue:{id}` e `{prefix}:venue:{id}:sections`.

**Checkpoint**: domínio puro e portas prontos (sem implementação). User stories podem começar.

---

## Phase 3: User Story 1 - Cadastrar Novo Local Físico de Espetáculo (Venue) (Priority: P1) 🎯 MVP

**Goal**: Admin com `ROLE_ADMIN` cria Venue com nome único não-vazio. Duplicatas → 409; nome vazio → 400; sem `ROLE_ADMIN` → 401/403. Cache-aside em leitura pública individual.

### Tests for User Story 1 (REQUIRED by FR-009) ⚠️

- [ ] T013 [P] [US1] Unit: `VenueName` rejeita null/vazio/só espaços — `test/unit/domain/VenueNameTest.java`
- [ ] T014 [P] [US1] Unit: `Venue` construído com `VenueName` válido preserva invariantes — `test/unit/domain/VenueDomainTest.java`
- [ ] T015 [P] [US1] Contrato REST (`@QuarkusTest`, mocks de porta): POST `/api/v1/admin/venues` → 201/409/400/401 — `test/rest/VenueAdminResourceTest.java`
- [ ] T016 [P] [US1] Integração Testcontainers PostgreSQL: `uq_venue_name` dispara 409 em corrida concorrente — `test/integration/VenueRepositoryTest.java`
- [ ] T017 [P] [US1] E2E P1: cria Venue admin → GET público 200 → repetir nome → 409 → confirma chave `{prefix}:venue:{id}` no Redis após o GET — `test/integration/VenueE2ETest.java`

### Implementation for User Story 1

- [ ] T018 [P] [US1] DTOs `adapter/in/dto/CreateVenueRequest.java`, `adapter/out/dto/VenueResponse.java`, `adapter/out/dto/VenuePageResponse.java`
- [ ] T019 [P] [US1] `application/mapper/VenueMapper.java` — `toEntity`, `toDomain(VenueEntity)`, `toResponse(Venue)`, mesmo padrão de `EventCategoryMapper`
- [ ] T020 [P] [US1] `application/port/in/CreateVenueUseCase.java`, `GetVenueUseCase.java`, `ListVenuesUseCase.java`
- [ ] T021 [US1] `application/usecase/VenueService.java` implementando `CreateVenueUseCase`/`GetVenueUseCase`/`ListVenuesUseCase` (e depois Update/Delete em US3/US4): valida nome, checa unicidade via porta, salva, invalida cache pós-sucesso — `@ApplicationScoped`, mesmo padrão de `EventCategoryService`
- [ ] T022 [US1] `adapter/out/persistence/PanacheVenueRepository.java` implementa `VenueRepositoryPort`
- [ ] T023 [US1] `adapter/out/cache/RedisVenueCache.java` implementa `VenueCachePort`, TTL via `@ConfigProperty("catalog.cache.venue-ttl")`
- [ ] T024 [US1] `adapter/rest/mapper/VenueExceptionMapper.java` (depende de T000 `ProblemDetails`): mapeia `VenueAlreadyExistsException`→409, `VenueNotFoundException`→404, `InvalidVenueInputException`→400
- [ ] T025 [US1] `adapter/rest/VenueAdminResource.java` — `@Path("/api/v1/admin/venues")`, `@RolesAllowed("ROLE_ADMIN")`: `@POST` (201)
- [ ] T026 [US1] `adapter/rest/VenueResource.java` — `@Path("/api/v1/venues")`, sem auth: `@GET` (200 paginado), `@GET /{id}` (200/404)

**Checkpoint**: US1 funcional e testável de forma independente.

---

## Phase 4: User Story 2 - Definir Seções Físicas Estruturais do Local (Priority: P2)

**Goal**: Admin adiciona Section a um Venue existente. `capacity` sempre derivada pelo banco, nunca aceita como entrada (FR-005a).

### Tests for User Story 2 (REQUIRED) ⚠️

- [ ] T027 [P] [US2] Unit: `Section` rejeita `numberOfRows<=0`/`rowCapacity<=0`; fábrica de criação não expõe parâmetro `capacity` — `test/unit/domain/SectionDomainTest.java`
- [ ] T028 [P] [US2] Contrato REST: POST sections → 201/404 (venue inexistente)/409 (nome duplicado)/400 (dims); payload com `capacity: 999` é ignorado, resposta traz capacidade calculada — `test/rest/SectionAdminResourceTest.java`
- [ ] T029 [P] [US2] Integração Testcontainers: `capacity` é de fato coluna gerada (SELECT nativo confirma); `uq_section_venue_name` dispara 409 — `test/integration/SectionCapacityAndUniqueConstraintTest.java`

### Implementation for User Story 2

- [ ] T030 [P] [US2] DTOs `adapter/in/dto/CreateSectionRequest.java` (`@JsonIgnoreProperties(ignoreUnknown = true)`, sem campo `capacity`), `adapter/out/dto/SectionResponse.java`
- [ ] T031 [P] [US2] `application/mapper/SectionMapper.java`
- [ ] T032 [P] [US2] `application/port/in/AddSectionUseCase.java`, `GetSectionUseCase.java`, `ListSectionsUseCase.java`
- [ ] T033 [US2] `application/usecase/SectionService.java`: valida venue existe (404), nome/dims (400), unicidade por venue (409), salva, re-lê para obter `capacity` gerado, invalida `evictSections(venueId)`
- [ ] T034 [US2] `adapter/out/persistence/PanacheSectionRepository.java` implementa `SectionRepositoryPort`
- [ ] T035 [US2] Estender `VenueExceptionMapper` (ou criar `SectionExceptionMapper`) com `SectionAlreadyExistsException`→409, `InvalidSectionInputException`→400
- [ ] T036 [US2] `adapter/rest/SectionAdminResource.java` — `@Path("/api/v1/admin/venues/{venueId}/sections")`, `@RolesAllowed("ROLE_ADMIN")`: `@POST` (201)
- [ ] T037 [US2] Endpoint público `@GET /api/v1/venues/{venueId}/sections` em `adapter/rest/SectionResource.java` (sem auth)

**Checkpoint**: US2 pronta; US1+US2 validáveis independentemente.

---

## Phase 5: User Story 3 - Alterar Cadastro de Local ou Seção (Priority: P3)

### Tests for User Story 3 (REQUIRED)

- [ ] T038 [P] [US3] Contrato REST: PUT venues (200/400/404/409), PUT sections (200/400/404/409); resposta de section reflete capacidade recalculada — `test/rest/VenueAndSectionUpdateTest.java`
- [ ] T039 [P] [US3] Integração Testcontainers: PUT section altera `number_of_rows`/`row_capacity` → SELECT nativo confirma nova `capacity` — `test/integration/UpdateCapacityTest.java`

### Implementation for User Story 3

- [ ] T040 [P] [US3] DTO `adapter/in/dto/UpdateVenueRequest.java`, `adapter/in/dto/UpdateSectionRequest.java`
- [ ] T041 [P] [US3] `application/port/in/UpdateVenueUseCase.java`, `UpdateSectionUseCase.java`
- [ ] T042 [US3] Estender `VenueService`/`SectionService` com `updateVenue`/`updateSection`: valida unicidade excluindo o próprio id, atualiza, invalida cache (venue + sections) síncrono
- [ ] T043 [US3] Preencher `@PUT /{id}` em `VenueAdminResource` e `@PUT /{sectionId}` em `SectionAdminResource`

**Checkpoint**: CRUD de Venue e Section completo, exceto DELETEs.

---

## Phase 6: User Story 4 - Excluir Local Físico sem Agendamentos (Priority: P4)

### Tests for User Story 4 (REQUIRED)

- [ ] T044 [P] [US4] Contrato REST: DELETE admin venues → 204/409 (show vinculado)/404 — `test/rest/VenueDeleteTest.java`
- [ ] T045 [P] [US4] Integração Testcontainers: DELETE sem shows → sections removidas em cascata; DELETE com show → `ON DELETE RESTRICT` dispara 409, rollback — `test/integration/VenueDeleteCascadeAndRestrictTest.java`

### Implementation for User Story 4

- [ ] T046 [US4] Estender `VenueService.deleteVenue`: checa `hasShows` (409 `VenueHasShowsException`), senão `deleteById` (cascade delegado ao Postgres), invalida cache
- [ ] T047 [US4] Preencher `@DELETE /{id}` em `VenueAdminResource` (204)

**Checkpoint**: exclusão de Venue delegada a FKs do banco; aplicação só valida `hasShows` para 409 amigável.

---

## Phase 7: User Story 5 - Excluir Seção Individual (Priority: P4)

### Tests for User Story 5 (REQUIRED)

- [ ] T048 [P] [US5] Contrato REST: DELETE `/admin/sections/{id}` → 204/404 — `test/rest/SectionDeleteTest.java`
- [ ] T049 [P] [US5] Integração: delete section não afeta Venue nem outras sections; cache de listagem invalidado — `test/integration/SectionDeleteTest.java`

### Implementation for User Story 5

- [ ] T050 [US5] Estender `SectionService.deleteSection`: busca section (404), captura `venueId`, `deleteById`, invalida `evictSections(venueId)`. Sem validação de vendas (fora de escopo — ver Riscos da spec).
- [ ] T051 [US5] Preencher `@DELETE` em `adapter/rest/SectionAdminResource.java` — `@Path("/api/v1/admin/sections/{sectionId}")`

**Checkpoint**: CRUD completo de Venue e Section.

---

## Phase 8: Polish & Cross-Cutting

- [ ] T052 [P] Corrigir `contracts/openapi.yaml`: substituir path único `/venues` por dois grupos de rota (`/api/v1/venues*` público, `/api/v1/admin/venues*` e `/api/v1/admin/sections/{id}` admin), alinhado ao padrão real de `event-categories`.
- [ ] T053 [P] Checks de consistência UX: todo erro em `application/problem+json`; paginação base 0; 401/403 corretos; UUIDs em todos os ids — `test/rest/UxConsistencyTest.java`
- [ ] T054 Smoke test local via `quickstart.md` atualizado (paths corrigidos para dois resources por entidade).
- [ ] T055 Rodar suíte completa (`./mvnw test`) — todos os testes unitários/contrato/integração/E2E devem passar antes de merge.

---

## Dependencies & Execution Order

- **Phase 0**: sem dependências, mas **bloqueia compilação do módulo** — deve rodar antes de qualquer teste que dependa de `ProblemDetails`.
- **Phase 1 (Setup)**: sem dependências.
- **Phase 2 (Foundational)**: depende de Phase 1. Bloqueia todas as US.
- **US1 (Phase 3)**: depende de Foundational.
- **US2 (Phase 4)**: depende de Foundational (Section-related) + repositório de Venue existir (Foundational já cobre `VenueRepositoryPort`). Pode rodar em paralelo com US1.
- **US3 (Phase 5)**: depende de US1 e US2 (endpoints PUT preenchem resources já criados).
- **US4 (Phase 6)**: depende de US1. Independente de US2/US3/US5.
- **US5 (Phase 7)**: depende de US2. Independente de US3/US4.
- **Polish (Phase 8)**: depende de todas as US entregues.

### Parallel Opportunities

- T002–T012: domínio/portas em paralelo (arquivos distintos)
- T013–T017: testes US1 em paralelo
- T018–T020: DTOs/mapper/portas-in US1 em paralelo
- T027–T029: testes US2 em paralelo
- T038–T039: testes US3 em paralelo
- T044–T045: testes US4 em paralelo
- T048–T049: testes US5 em paralelo

## Implementation Strategy

### MVP First (apenas US1)

1. Phase 0 (bloqueante) → Phase 1 (Setup) → Phase 2 (Foundational, apenas itens de Venue: T002–T005, T009 parcial, T010, T012)
2. Phase 3 completa (testes + implementação)
3. Validar E2E P1 (T017), rodar suíte
4. Demo: admin cadastra locais físicos

### Incremental Delivery

Setup+Foundational → US1 (MVP) → US2 (seções + capacidade) → US3 (updates) → US4 (delete venue) → US5 (delete section) → Polish.

## Notes

- Reaproveitar `VenueEntity`/`SectionEntity`/`VenueAddressEmbeddable` **exatamente como estão** — não recriar em outro pacote.
- Riscos conhecidos (ver `spec.md` "Riscos"): sem evento `SectionDeleted`/`VenueDeleted` para `inventory`; validação de capacidade vs. tickets vendidos fora de escopo. **Não** adicionar código para esses pontos nesta feature.
- T000/T000a são pré-requisitos de build herdados de outra feature — sinalizar no PR, não silenciar.