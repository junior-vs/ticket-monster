# Tasks: Gerenciar Locais Físicos e Seções Estruturais (Admin)

**Input**: Design documents from `/specs/010-gerenciar-locais-secoes/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/openapi.yaml, quickstart.md

**Tests**: Testes são OBRIGATÓRIOS por Constituição III (E2E P1) e por FR-009 (unitários de domínio, contrato REST, integração Testcontainers Postgres/Redis, E2E P1).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4, US5)
- Include exact file paths in descriptions

## Path Conventions

- Project root: `microservice-catalog/` (Maven module)
- Sources: `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/`
- Tests: `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/venue/`
- Resources: `microservice-catalog/src/main/resources/application.properties` and `db/migration/V1__init.sql` (já contém DDL)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Pacote e estrutura base do agregado Venue/Section, properties de configuração. O projeto Quarkus já existe (pom.xml, db/migration/V1__init.sql com DDL de venue, section, show). Nenhuma nova migração é necessária.

- [ ] T001 Criar estrutura de pacotes do agregado `catalog.venue.*` (domain, application, adapter/in/rest/dto, adapter/in/rest/exception, adapter/out/persistence/entity, adapter/out/cache, adapter/out/persistence) em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/`
- [ ] T002 [P] Criar estrutura de pacotes de testes do agregado `catalog.venue.*` (domain/, application/, adapter/in/rest/, adapter/out/persistence/, adapter/out/cache/) em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/venue/`
- [ ] T003 [P] Adicionar propriedades de configuração do Venue em `microservice-catalog/src/main/resources/application.properties`: `catalog.venue.cache.ttl-seconds=3600` (TTL do cache Redis individual e listagem de sections)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Domínio puro (entidades + portas de saída), panache entities (JPA mapping), ports de repositório e cache SEM lógica de aplicação. Bloqueia todas as histórias de usuário.

**⚠️ CRITICAL**: Nenhuma US pode começar sem: Venue/Section de domínio puro, VenueEntity/SectionEntity Panache, portas de saída e readers de cache.

- [ ] T004 [P] Criar entidade de DOMÍNIO PURO `Venue` (aggregate root, SEM annotations JPA/ORM) em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/domain/Venue.java`: campos `id UUID`, `name String (not blank)`, `description String`, `addressLine String (nullable)`, `city String (nullable)`, `state String (nullable)`, `postalCode String (nullable)`, `country String (nullable)`, `createdAt OffsetDateTime`. Invariante: `name` não null nem branco (validado por static factory ou método de validação de domínio).
- [ ] T005 [P] Criar entidade de DOMÍNIO PURO `Section` (filha de Venue, SEM annotations JPA/ORM) em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/domain/Section.java`: campos `id UUID`, `venueId UUID`, `name String (not blank)`, `numberOfRows Integer (>0)`, `rowCapacity Integer (>0)`, `capacity Integer (read-only, sempre derivado, NUNCA aceito em construtor/fábrica)`. Invariantes: `name` not blank; `numberOfRows > 0`; `rowCapacity > 0`; `capacity` SEM setter nem parâmetro de entrada (campo read-only acessível apenas via getter).
- [ ] T006 [P] Criar porta de saída `VenueRepositoryPort` (interface SEM dependências) em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/domain/VenueRepositoryPort.java`: métodos `Uni<Venue> save(Venue v)`, `Uni<Venue> findById(UUID id)`, `Uni<Boolean> existsByName(String name, UUID excludeIdIfAny)`, `Uni<Boolean> existsById(UUID id)`, `Uni<Venue> update(Venue v)`, `Uni<Page<Venue>> findAll(int page, int size)`, `Uni<Boolean> hasShows(UUID id)` (valida show vinculado para DELETE RESTRICT), `Uni<Void> deleteById(UUID id)`.
- [ ] T007 [P] Criar porta de saída `SectionRepositoryPort` (interface SEM dependências) em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/domain/SectionRepositoryPort.java`: métodos `Uni<Section> save(Section s)`, `Uni<Section> findById(UUID id)`, `Uni<List<Section>> findByVenueId(UUID venueId)`, `Uni<Page<Section>> findByVenueIdPaged(UUID venueId, int page, int size)`, `Uni<Boolean> existsByNameInVenue(String name, UUID venueId, UUID excludeIdIfAny)`, `Uni<Section> update(Section s)`, `Uni<Void> deleteById(UUID id)`.
- [ ] T008 [P] Criar porta de saída `VenueCachePort` (interface SEM dependências Redis) em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/domain/VenueCachePort.java`: métodos `Uni<Venue> getVenue(UUID id)`, `Uni<Void> setVenue(Venue v, Duration ttl)`, `Uni<Void> evictVenue(UUID id)`, `Uni<List<Section>> getSections(UUID venueId)`, `Uni<Void> setSections(UUID venueId, List<Section> sections, Duration ttl)`, `Uni<Void> evictSections(UUID venueId)`.
- [ ] T009 [P] Criar Panache entity `VenueEntity` (Hibernate Reactive Panache) em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/adapter/out/persistence/entity/VenueEntity.java`: mapear schema `catalog.venue`, colunas (`id UUID pk`, `name varchar(120) unique`, `description text`, `address_line`, `city`, `state`, `postal_code`, `country`, `created_at timestamptz`), relations `@OneToMany(mappedBy = "venue", cascade = ALL, orphanRemoval = true) List<SectionEntity> sections`.
- [ ] T010 [P] Criar Panache entity `SectionEntity` (Hibernate Reactive Panache) em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/adapter/out/persistence/entity/SectionEntity.java`: mapear schema `catalog.section`, colunas (`id UUID pk`, `venue_id UUID FK catalog.venue ON DELETE CASCADE`, `name varchar(120)`, `number_of_rows int`, `row_capacity int`, `capacity int GENERATED ALWAYS AS (number_of_rows * row_capacity) STORED — campo `capacity` com `@Column(insertable = false, updatable = false)` e `@Generated` ou equivalente para leitura após persist). Unique constraint composta `(venue_id, name)` mapeada via `@Table(uniqueConstraints=...)`. FK Many-to-One `venue`.
- [ ] T011 [P] Adicionar ShowEntity minimal reference ou query nativa para `hasShows(UUID venueId)` em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/adapter/out/persistence/entity/` (criar um ShowEntity apenas com `@Id UUID id` e `@ManyToOne VenueEntity venue` mapeando `catalog.show` — ou usar Panache.nativeQuery no repositório se preferir não criar entidade)
- [ ] T012 Implementar adapter de persistência `PanacheVenueRepository` implementa `VenueRepositoryPort` em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/adapter/out/persistence/PanacheVenueRepository.java`: mapper Venue ↔ VenueEntity (inclui list<Section> ↔ list<SectionEntity> em cascata para hasShows, findAll pages, etc). Para `hasShows`: native query ou ShowEntity contagem count. `@ApplicationScoped` e usar `PanacheEntityBase` / `Uni` pipelines.
- [ ] T013 Implementar adapter de persistência `PanacheSectionRepository` implementa `SectionRepositoryPort` em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/adapter/out/persistence/PanacheSectionRepository.java`: mapper Section ↔ SectionEntity. Unique name per venue com `existsByNameInVenue`: native query `SELECT 1 FROM catalog.section WHERE venue_id = $1 AND LOWER(name) = LOWER($2) [AND id <> $3]`.
- [ ] T014 Implementar adapter de cache `RedisVenueCache` implementa `VenueCachePort` em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/adapter/out/cache/RedisVenueCache.java`: usar `RedisClient` / `ReactiveRedisDataSource` Quarkus; chaves `catalog:venue:{id}` (serializar Venue como JSON usando Jackson ObjectMapper), `catalog:venue:{id}:sections` (serializar List<Section> como JSON). TTL lido de `@ConfigProperty(name = "catalog.venue.cache.ttl-seconds") Duration`.

**Checkpoint**: Foundation ready — entidades de domínio puro (sem framework), portas de saída (sem implementação), panache entities (JPA mapping), implementações Panache repositórios, implementação cache Redis. User story implementation can now begin.

---

## Phase 3: User Story 1 - Cadastrar Novo Local Físico de Espetáculo (Venue) (Priority: P1) 🎯 MVP

**Goal**: Admin autenticado com `ROLE_ADMIN` cria Venue com nome único não-vazio, descrição e endereço opcional. Sistema persiste, retorna 201 Created com UUID. Duplicatas → 409 Conflict; nome vazio/branco → 400; sem ROLE_ADMIN → 401/403. Uso de cache-aside Redis para leitura individual (miss primeiro → banco → set cache).

**Independent Test**: Executar cenários de quickstart.md US1 (Cenário 1, 3a, 3b, 4): POST `/admin/venues` com token admin válido retorna 201; POST com nome duplicado retorna 409; POST com nome "   " retorna 400; POST sem token retorna 401. Todos os erros com RFC 7807. Cache Redis catalog:venue:{id} preenchido após primeiro GET público.

### Tests for User Story 1 (REQUIRED by FR-009) ⚠️

> **NOTE: Escrever os testes PRIMEIRO, garantir que FALHEM antes da implementação.**

- [ ] T015 [P] [US1] Teste unitário de domínio: `VenueValidator` / fábrica estática de `Venue` rejeita nome null, vazio ou só brancos em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/venue/domain/VenueDomainTest.java`
- [ ] T016 [P] [US1] Teste unitário de domínio: validador de regra de unicidade prévio (lógica pura que recebe `existsByNameSupplier` e retorna exceção de domínio) em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/venue/domain/VenueUniquenessTest.java`
- [ ] T017 [P] [US1] Teste de contrato REST (REST Assured valida schema response) para POST `/api/v1/admin/venues`: (a) 201 Created com campos obrigatórios e UUID; (b) 409 Conflict com Problem `title="Nome de local já cadastrado"`; (c) 400 Bad Request nome em branco; (d) 401 sem token — em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/venue/adapter/in/rest/VenueAdminResourceContractTest.java` (usar `@QuarkusTest` sem Testcontainers — stubs repo em CDI se necessário ou @InjectMock)
- [ ] T018 [P] [US1] Teste de integração com Testcontainers PostgreSQL: constraint `uq_venue_name` do banco dispara 409 em corrida concorrente (duas threads criando mesmo nome) garantindo que a pré-validação não é a única proteção — em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/venue/adapter/out/persistence/VenueRepositoryIT.java`
- [ ] T019 [P] [US1] Teste E2E P1 fluxo completo: start Quarkus com Postgres+Redis via Testcontainers; admin cria venue com nome único; GET público `/venues/{id}` retorna 200 e valores batem; nova POST com mesmo nome retorna 409; validar chave `catalog:venue:{id}` existe no Redis após o primeiro GET público — em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/venue/VenueE2ETest.java`

### Implementation for User Story 1

- [ ] T020 [P] [US1] Criar DTOs REST de entrada e saída: `CreateVenueRequest` (name obrigatório, description, addressLine, city, state, postalCode, country — sem `id`, sem `createdAt`), `VenueResponse` (id, name, description, addressLine, city, state, postalCode, country, createdAt), `VenuePageResponse` (content[], page, size, totalElements) em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/adapter/in/rest/dto/`
- [ ] T021 [P] [US1] Implementar caso de uso `CreateVenueUseCase` em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/application/CreateVenueUseCase.java`: recebe nome, endereço, descrição; valida `name` not blank (throw `InvalidVenueNameException`); consulta `venueRepository.existsByName(name, null)`; se existir throw `DuplicateVenueNameException`; cria Venue (UUID aleatório ou null para gerar); save via repository; retorna Venue; SEM dependências HTTP/Redis (cache invalidado pelo chamador/adapter se necessário). @ApplicationScoped, injeta `VenueRepositoryPort`.
- [ ] T022 [P] [US1] Implementar caso de uso `GetVenueUseCase` em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/application/GetVenueUseCase.java`: cache-aside — primeiro `venueCache.getVenue(id)`; se miss → `venueRepository.findById(id)`; se hit no banco → `venueCache.setVenue(venue, ttl)`; retorna Optional/Venue (lança `VenueNotFoundException` 404 se não existir). Injeta ambas portas.
- [ ] T023 [P] [US1] Implementar caso de uso `ListVenuesUseCase` em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/application/ListVenuesUseCase.java`: paginação `page` base 0, `size` (default 20, validação size 1..100). Apenas consulta repo (listagens não cacheadas nesta feature). Injeta `VenueRepositoryPort`.
- [ ] T024 [US1] Implementar exceções de domínio mapeadas para RFC 7807: `InvalidVenueNameException` (status 400, title "Nome de local inválido"), `DuplicateVenueNameException` (status 409, title "Nome de local já cadastrado"), `VenueNotFoundException` (status 404, title "Local não encontrado"), `UnauthorizedException`, `ForbiddenException` ou reutilizar exceptions JAX-RS oidc. Exception mapper `VenueExceptionMapper extends ExceptionMapper<Throwable>` implementa `@Provider` para converter exceções de domínio em `Problem` `application/problem+json` em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/adapter/in/rest/exception/VenueExceptionMapper.java`. Deve também capturar `ConstraintViolationException` (Hibernate) e mapear para 409 se for `uq_venue_name`.
- [ ] T025 [US1] Implementar `VenueAdminResource` (admin ROLE_ADMIN) em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/adapter/in/rest/VenueAdminResource.java`: `@Path("/api/v1/admin/venues")`, `@RolesAllowed("ROLE_ADMIN")` na classe. Endpoints: `@POST` create (201 Created com Location header, body VenueResponse), `@GET` paginado (200), `@GET /{id}` (200/404), `@PUT /{id}` (T038 US3), `@DELETE /{id}` (T050 US4). Mapeamento DTO ↔ Venue dentro do resource ou em mapper dedicado package-private.
- [ ] T026 [US1] Implementar `VenuePublicResource` (público, SEM auth) em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/adapter/in/rest/VenuePublicResource.java`: `@Path("/api/v1/venues")`, sem `@RolesAllowed`. Endpoints: `@GET` paginado (200 VenuePageResponse), `@GET /{id}` (200 VenueResponse ou 404 Problem). Usar mesmos UseCases `GetVenueUseCase` e `ListVenuesUseCase`.

**Checkpoint**: US1 funcional e independentemente testável. Endpoints admin/public de Venue, cache-aside individual, erros RFC 7807 para nome duplicado/branco, auth ROLE_ADMIN.

---

## Phase 4: User Story 2 - Definir Seções Físicas Estruturais do Local (Priority: P2)

**Goal**: Admin adiciona Section a um Venue existente. Section tem nome único DENTRO do Venue, `number_of_rows > 0`, `row_capacity > 0`. `capacity` é SEMPRE coluna gerada (GENERATED ALWAYS ... STORED) — NUNCA aceito como entrada, nunca calculado pela aplicação. Payload contendo `capacity` deve ser IGNORADO (FR-005a). Inclui listagem pública de Sections por Venue, cache-aside chave `catalog:venue:{id}:sections`.

**Independent Test**: POST `/admin/venues/{venueId}/sections` retorna 201 e `capacity` = numberOfRows*rowCapacity calculado pelo banco (verificar SELECT via Panache ou response); nome duplicado no mesmo venue retorna 409; `number_of_rows` <= 0 ou `row_capacity` <= 0 retorna 400; payload com `capacity: 999` é ignorado, response tem capacity correto; venue inexistente retorna 404. Cache Redis `catalog:venue:{id}:sections` preenchido no primeiro GET público.

### Tests for User Story 2 (REQUIRED) ⚠️

> Primeiro escrever, garantir falha.

- [ ] T027 [P] [US2] Teste unitário de domínio: `Section` invariantes — `name` not blank, `numberOfRows > 0`, `rowCapacity > 0`, `capacity` NÃO aceito como entrada (sem setter, sem construtor param). Tentar construir section com campos negativos ou name branco lança exceção de domínio — em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/venue/domain/SectionDomainTest.java`
- [ ] T028 [P] [US2] Teste unitário de domínio: `Section` static factory / mapper de entrada IGNORA campo `capacity` explicitamente mesmo que fornecido (injeção de campo "excedente" é descartado) — em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/venue/domain/SectionFactoryCapacityIgnoredTest.java`
- [ ] T029 [P] [US2] Teste contrato REST `SectionAdminResource`: POST sections 201, 404 venue inexistente, 409 nome duplicado, 400 rows/capacity <= 0, e CRÍTICO: POST com `"capacity": 999` retorna 201 mas response mostra `capacity` calculado (10×8=80, por exemplo) NÃO 999 — em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/venue/adapter/in/rest/SectionAdminResourceContractTest.java`
- [ ] T030 [P] [US2] Teste integração Testcontainers PostgreSQL: `capacity` coluna GERADA de fato — INSERT via repo, SELECT usando Panache/SQL nativo para confirmar que o valor não vem da aplicação. Tentar violar `uq_section_name_per_venue` dispara constraint (409 via mapper) — em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/venue/adapter/out/persistence/SectionCapacityAndUniqueConstraintIT.java`

### Implementation for User Story 2

- [ ] T031 [P] [US2] Criar DTOs Section: `CreateSectionRequest` (name required, numberOfRows required>0, rowCapacity required>0; **sem campo capacity** para documentar — mas mesmo que alguém envie, o adapter deve ignorá-lo via `@JsonIgnoreProperties(ignoreUnknown = true)` e também a camada de aplicação NÃO lê nenhum capacity de entrada), `UpdateSectionRequest` (T041 US3), `SectionResponse` (id, venueId, name, numberOfRows, rowCapacity, capacity read-only), `SectionPageResponse` em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/adapter/in/rest/dto/`
- [ ] T032 [P] [US2] Implementar caso de uso `AddSectionUseCase` em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/application/AddSectionUseCase.java`: recebe venueId, name, numberOfRows, rowCapacity. Passos: (1) validar venue existe via `venueRepository.existsById(venueId)` → senão 404; (2) validar name not blank, rows>0, rowCap>0 → 400; (3) validar único por venue via `sectionRepository.existsByNameInVenue(name, venueId, null)` → 409; (4) criar Section (nova UUID, venueId, fields); (5) `sectionRepository.save(section)` (lembrando: capacity é coluna gerada então retorno do save deve re-read/refresh para pegar capacity calculado pelo banco — `Panache.getSession().flush() + refresh()` ou `repository.findById()` após save se necessário); (6) invalidar cache `venueCache.evictSections(venueId)` sincronamente pós-sucesso. Retorna Section (com capacity já lido do banco). Injeta ambas portas + cache.
- [ ] T033 [P] [US2] Implementar caso de uso `ListSectionsUseCase` em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/application/ListSectionsUseCase.java`: cache-aside para listagem completa: `venueCache.getSections(venueId)` → se miss → verificar venue existe (404 se não) → `sectionRepository.findByVenueId(venueId)` (ou paginado, paginado SEM cache) → set cache; retorna. Injeta ambas portas + cache.
- [ ] T034 [US2] Adicionar exceções de domínio no `VenueExceptionMapper` (ou criar SectionExceptionMapper se manter separado): `InvalidSectionNameException` (400), `InvalidSectionDimensionsException` (400 "Número de fileiras e capacidade por fileira devem ser > 0"), `DuplicateSectionNameInVenueException` (409 "Nome de seção já cadastrado neste local"). Mapear `ConstraintViolationException` de `uq_section_name_per_venue` para 409 também.
- [ ] T035 [US2] Criar `SectionAdminResource` admin em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/adapter/in/rest/SectionAdminResource.java`: `@Path("/api/v1/admin/venues/{venueId}/sections")` `@RolesAllowed("ROLE_ADMIN")`. `@POST` retorna 201 Created (Location = `/admin/sections/{id}` ou `/admin/venues/{venueId}/sections/{id}`). `@PUT /{sectionId}` e `@DELETE /admin/sections/{sectionId}` serão preenchidos em US3/US5.
- [ ] T036 [US2] Adicionar endpoint público `@GET /api/v1/venues/{venueId}/sections` no `VenuePublicResource` (ou criar `SectionPublicResource`): retorna 200 SectionPageResponse ou lista; usa `ListSectionsUseCase`; 404 se venue não existe.

**Checkpoint**: US2 pronta — criação de sections com nome único por venue, rows/rowCap>0, capacity SEMPRE calculada pelo banco, cache de listagem, erros RFC 7807. US1 + US2 podem ser validadas independentemente.

---

## Phase 5: User Story 3 - Alterar Cadastro de Local ou Seção (Priority: P3)

**Goal**: Admin edita Venue (nome, descrição, endereço) ou Section (nome, numberOfRows, rowCapacity). Alterações de `numberOfRows`/`rowCapacity` RECALCULAM `capacity` VIA BANCO (aplicação não calcula). Alteração de nome de Venue: conflito → 409. Alteração de nome de Section: conflito no mesmo venue → 409. Invalidação síncrona de cache.

**Independent Test**: PUT `/admin/venues/{id}` altera dados; PUT com nome igual a outro venue → 409. PUT sections altera rows/rowCap e response mostra capacity NOVO (banco recalculou). PUT section renomeia para nome já existente no mesmo venue → 409. Cache individual e sections são limpos (confirmar Redis após update).

### Tests for User Story 3 (REQUIRED)

- [ ] T037 [P] [US3] Teste contrato REST: PUT venues (200, 400 nome branco, 409 conflito renome, 404 inexistente), PUT sections (200, 400 dims, 409 conflito nome, 404, 404 venue inexistente path). Validar que section PUT response capacity = novo rows*novo rowCap — em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/venue/adapter/in/rest/VenueAndSectionUpdateContractTest.java`
- [ ] T038 [P] [US3] Teste integração Testcontainers: alterar section rows/rowCap → executar SELECT nativo confirmar capacity do banco é novo valor, não o antigo; alterar nome de Venue em corrida para validar constraint `uq_venue_name` também pega concorrência em update — em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/venue/adapter/out/persistence/UpdateConstraintsAndCapacityIT.java`

### Implementation for User Story 3

- [ ] T039 [P] [US3] Criar `UpdateVenueRequest` DTO em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/adapter/in/rest/dto/UpdateVenueRequest.java` (name, description, addressLine, city, state, postalCode, country — mesmas regras de validação de criação)
- [ ] T040 [P] [US3] Implementar caso de uso `UpdateVenueUseCase` em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/application/UpdateVenueUseCase.java`: get venue existente (404 se não), validar novo nome not blank, validar unicidade `existsByName(newName, id)` (exclude self), atualizar campos description/endereço, `venueRepository.update(venue)`, `venueCache.evictVenue(id)` e `venueCache.evictSections(id)` invalidar ambos.
- [ ] T041 [P] [US3] Criar `UpdateSectionRequest` DTO em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/adapter/in/rest/dto/UpdateSectionRequest.java` (name, numberOfRows>0, rowCapacity>0 — NÃO aceita capacity; `@JsonIgnoreProperties(ignoreUnknown = true)`)
- [ ] T042 [P] [US3] Implementar caso de uso `UpdateSectionUseCase` em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/application/UpdateSectionUseCase.java`: recebe sectionId, venueId (path), nome, rows, rowCap. (1) validar venue existe (404); (2) buscar section existente (404 se não); (3) validar section.venueId bate com path (404/400 se não — não permitir section de outro venue via URL); (4) validar nome not blank, dims>0 (400); (5) unicidade `existsByNameInVenue(newName, venueId, sectionId)` excluir self → 409; (6) atualizar campos na entidade Section; (7) `sectionRepository.update(section)`; (8) refresh/findById para pegar novo capacity do banco (coluna gerada); (9) invalidar `venueCache.evictSections(venueId)` sincronamente.
- [ ] T043 [US3] Preencher `@PUT /{id}` em `VenueAdminResource` (UpdateVenueUseCase → 200 VenueResponse) e `@PUT /{sectionId}` em `SectionAdminResource` (UpdateSectionUseCase → 200 SectionResponse).

**Checkpoint**: US3 concluída; CRUD Venue e Section completo (exceto DELETEs de US4 e US5). Toda alteração invalida caches síncronamente.

---

## Phase 6: User Story 4 - Excluir Local Físico sem Agendamentos (Priority: P4)

**Goal**: Admin exclui Venue. Se tiver shows vinculados → 409 (ON DELETE RESTRICT FK `show.venue_id`). Se não tiver → 204 e Sections deletadas em cascata (ON DELETE CASCADE FK `section.venue_id`, handled pelo Postgres). Invalidação de cache (venue + sections) síncrona.

**Independent Test**: Criar venue sem shows → DELETE retorna 204; sections somem do banco; cache evicted. Criar venue + inserir show (inserir via SQL nativo ou ShowEntity mínima) → DELETE retorna 409 Problem; venue e sections permanecem.

### Tests for User Story 4 (REQUIRED)

- [ ] T044 [P] [US4] Teste contrato REST: DELETE admin venues 204 ok, 409 com show vinculado, 404 inexistente — em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/venue/adapter/in/rest/VenueDeleteContractTest.java`
- [ ] T045 [P] [US4] Teste integração Testcontainers PostgreSQL: DELETE venue SEM shows → valida sections deletadas em cascata (`SELECT count(*) FROM catalog.section WHERE venue_id = $1` → 0). DELETE COM shows → FK `show.venue_id ON DELETE RESTRICT` dispara exception; transação rollback; venue e sections permanecem. Cache evicted quando delete passa — em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/venue/adapter/out/persistence/VenueDeleteCascadeAndRestrictIT.java`

### Implementation for User Story 4

- [ ] T046 [P] [US4] Implementar caso de uso `DeleteVenueUseCase` em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/application/DeleteVenueUseCase.java`: (1) existe venue? 404 senão; (2) `venueRepository.hasShows(venueId)` — se true → `VenueHasShowsException` → 409 "Local possui espetáculos agendados e não pode ser excluído"; (3) se safe: `venueRepository.deleteById(venueId)` (ON DELETE CASCADE deleta sections automaticamente pelo Postgres). (4) pós-sucesso: `venueCache.evictVenue(venueId)` e `venueCache.evictSections(venueId)` invalidar síncrono.
- [ ] T047 [US4] Adicionar exceção `VenueHasShowsException` mapeada para 409 no `VenueExceptionMapper`
- [ ] T048 [US4] Preencher `@DELETE /{id}` em `VenueAdminResource` (DeleteVenueUseCase → 204 No Content).

**Checkpoint**: US4 concluída. Exclusão de venue com RESTRICT/CASCADE 100% delegada a FKs do banco; aplicação só valida prévio de shows para retornar 409 amigável. Cache inválido.

---

## Phase 7: User Story 5 - Excluir Seção Individual (Priority: P4)

**Goal**: Admin exclui Section individual sem excluir Venue. Microservice-catalog não tem visibilidade de ingressos vendidos (não depende de inventory). DELETE sempre retorna 204 se section existe. Risco de propagação para inventory é documentado como risco arquitetural — nenhum evento Kafka publicado nesta feature. Invalidação de cache `catalog:venue:{venueId}:sections` síncrona.

**Independent Test**: Criar venue com duas seções; DELETE `/admin/sections/{id1}` → 204; validar via GET público sections que somente seção 2 permanece; cache `catalog:venue:{id}:sections` foi evicted ou atualizado (miss → banco → nova lista sem seção 1). Section inexistente → 404.

### Tests for User Story 5 (REQUIRED)

- [ ] T049 [P] [US5] Teste contrato REST: DELETE `/admin/sections/{id}` 204, 404 seção inexistente; validar listagem sections após delete sem a seção deletada — em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/venue/adapter/in/rest/SectionDeleteContractTest.java`
- [ ] T050 [P] [US5] Teste integração: delete section + confirmar venue permanece (não afetado); cache sections inválido (primeiro GET público miss, depois cache preenchido sem a seção) — em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/venue/adapter/out/persistence/SectionDeleteVenuePreservedIT.java`

### Implementation for User Story 5

- [ ] T051 [P] [US5] Implementar caso de uso `DeleteSectionUseCase` em `../../src/main/java/br/vsjr/labs/ticketmonster/catalog/venue/application/DeleteSectionUseCase.java`: (1) find section by id → 404 se não; (2) capturar `venueId` da section para invalidação cache; (3) `sectionRepository.deleteById(id)`; (4) pós-sucesso: `venueCache.evictSections(venueId)`. Sem validação de shows/ticketPrices neste serviço (não temos visibilidade).
- [ ] T052 [US5] Preencher `@DELETE` em `SectionAdminResource`: path separado `@Path("/api/v1/admin/sections/{sectionId}")` (conforme OpenAPI) ou `@DELETE /admin/venues/{venueId}/sections/{sectionId}`; usar DeleteSectionUseCase → 204 No Content.

**Checkpoint**: US5 concluída. CRUD completo de Sections (create/read/update/delete individual) e CRUD de Venue. Toda feature de Gerenciar Locais e Seções está implementada.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Cobertura de testes final, validação de performance e consistência UX, execução do quickstart.md.

- [ ] T053 [P] Unittest de validadores de domínio adicionais (caso não coberto): edge case `city=123 caracteres` (maiores que DDL), campos nulos opcionais (endereço) permitidos — em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/venue/domain/`
- [ ] T054 [P] Contract compatibility: `contracts/openapi.yaml` schema comparado com respostas reais (ferramenta: rodar teste que lê o YAML e valida DTOs via `@ServerTest` ou schema validator). Ou pelo menos manual check de `CreateSectionRequest` não contém `capacity`. Valores auth e RFC 7807.
- [ ] T055 [P] UX consistency checks: Validação em testes que todos erros usam `application/problem+json` com `type`, `title`, `status`, `detail`, `instance`; paginação base 0; auth retorna 401/403 corretamente; UUIDs em todos os ids — em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/venue/adapter/in/rest/UxConsistencyTest.java`
- [ ] T056 [P] Métricas e health: Confirmar `catalog.venue.*` tem micrometer timers (opcional) ou pelo menos `/q/health` e `/q/metrics` acessíveis após execução. Se necessário adicionar `@Metered` ou `@Timed` nos resources admin.
- [ ] T057 Smoke test local: executar `docker compose -f docker-compose.shared.yml up -d` e `.\mvnw.cmd quarkus:dev` no módulo; executar os 5 cenários de quickstart.md manualmente ou via script. Validar:
  - Cenário 1 US1: criar Venue sucesso 201
  - Cenário 2 US2: criar Section sucesso e capacity correto
  - Cenário 3 US3: atualizar Venue e Section sucesso
  - Cenário 4 US4: excluir Venue sem show → 204; com show → 409
  - Cenário 5 US5: excluir Section individual → 204
- [ ] T058 [P] Rodar bateria de testes automatizados completo: `.\mvnw.cmd test -pl microservice-catalog` (ou `.\mvnw.cmd failsafe:integration-test` se ITs separados por fase) — garantir que **TODOS** os testes unitários/contrato/IT/E2E P1 passam antes de merge.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sem dependências — começa imediatamente
- **Foundational (Phase 2)**: Depende de Setup (T001/T002/T003) concluído — BLOQUEIA todas as user stories
- **US1 (Phase 3)**: Depende de Foundational concluído. BLOQUEIA parcialmente US2 (Section precisa de Venue existente nos casos de uso, mas US1 e US2 são implementáveis em paralelo desde que os UseCases aceitem IDs de venue "que virão"; porém para testes independentes US2 depende de repositório de Venue existir — e T012 já existe em Foundational. Logo US1 e US2 podem ser paralelos por equipes diferentes).
- **US2 (Phase 4)**: Depende de Foundational (T005, T007, T010, T013, T014 Section-related concluídos) + US1's Venue repos/infrastructure (já existe em Foundational). Pode ser paralelo a US1.
- **US3 (Phase 5)**: Depende de US1 e US2 (depende de UpdateVenueResource e UpdateSectionResource existirem; T025 e T035). Não precisa de US4/US5.
- **US4 (Phase 6)**: Depende de US1 (VenueResource existe) e T006 (porta hasShows) + T011 ShowEntity. Independente de US2/US3/US5.
- **US5 (Phase 7)**: Depende de US2 (SectionResource existe). Independente de US3/US4.
- **Polish (Phase 8)**: Depende de todas as user stories concluídas ou das que serão entregues.

### User Story Dependencies (MVP → Full)

- **User Story 1 (P1)**: Após Foundational. Independente — MVP!
- **User Story 2 (P2)**: Após Foundational (e Section-related tasks T005/T007/T010/T013). Não precisa de US1 para implementar (pode criar Venue por SQL/setup nos testes), mas para entrega funcional completa: US1 é admin creating venue, US2 adding sections.
- **User Story 3 (P3)**: Depende de US1 e US2 (update PUTs só fazem sentido com resources existentes; `@PUT` path declarado em resources de US1/US2).
- **User Story 4 (P4)**: Depende de US1 (VenueRepository com hasShows, VenueResource DELETE path). Pode ser feito em paralelo com US2/US3.
- **User Story 5 (P4)**: Depende de US2 (SectionRepository, SectionResource DELETE path). Pode ser feito em paralelo com US3/US4.

### Within Each User Story

1. Escrever testes (unitários domínio, contrato REST, integração/E2E) PRIMEIRO — todos falham
2. Implementar DTOs e entidades de domínio
3. Implementar UseCases (regras de negócio)
4. Implementar Resources (REST adapters + exception mapping)
5. Executar testes do passo 1 — todos passam
6. Checkpoint: história independentemente validada

### Parallel Opportunities (tarefas marcadas [P])

- T001, T002, T003: Setup paralelo por arquivos
- T004-T011: Foundational paralelo por entidades/portas/panache entities
- T012 e T013: Panache repos paralelos
- T015-T019: Testes US1 paralelos
- T020, T021, T022, T023: DTOs + UseCases US1 paralelos entre si
- T027-T030: Testes US2 paralelos
- T031, T032, T033: DTOs + UseCases US2 paralelos
- T037, T038: Testes US3 paralelos; T039/T040/T041/T042 em paralelo
- T044 e T045: Testes US4 em paralelo
- T049 e T050: Testes US5 em paralelo
- T053-T058 Polish tasks em paralelo

---

## Parallel Example: User Story 1

```text
# Launch all tests for US1 together (first - TDD):
T015: VenueDomainTest.java
T016: VenueUniquenessTest.java
T017: VenueAdminResourceContractTest.java
T018: VenueRepositoryIT.java (Testcontainers)
T019: VenueE2ETest.java (Testcontainers P1 E2E)

# Launch implementation components in parallel after test scaffolding:
T020: Criar CreateVenueRequest.java, VenueResponse.java, VenuePageResponse.java
T021: Implementar CreateVenueUseCase.java
T022: Implementar GetVenueUseCase.java
T023: Implementar ListVenuesUseCase.java
```

## Parallel Example: User Story 2 (pode executar junto com US1 em times de 2+ devs)

```text
# US2 tests (TDD):
T027: SectionDomainTest.java
T028: SectionFactoryCapacityIgnoredTest.java
T029: SectionAdminResourceContractTest.java
T030: SectionCapacityAndUniqueConstraintIT.java

# US2 implementation components:
T031: Criar CreateSectionRequest.java, UpdateSectionRequest.java, SectionResponse.java, SectionPageResponse.java
T032: AddSectionUseCase.java
T033: ListSectionsUseCase.java
```

---

## Implementation Strategy

### MVP First (Only User Story 1 P1)

1. Concluir Phase 1 (Setup)
2. Concluir Phase 2 (Foundational) — tasks de Venue (T004, T006, T008, T009, T012) e tasks gerais (T014 cache) — não precisa de Section entities para MVP US1
3. Concluir Phase 3 (User Story 1 COMPLETA: tests + implementation)
4. **PARAR E VALIDAR**: Rodar Cenário 1 do quickstart.md E2E; validar testes unitários/contrato/IT/E2E passam
5. Deploy/Demo como MVP. US1 entrega valor: admin cadastra locais físicos.

### Incremental Delivery

1. Setup + Foundational → Base pronta
2. US1 (P1) → MVP: Cadastro de Locais. Validar, demo/deploy.
3. US2 (P2) → Seções + capacidade calculada. Validar, demo/deploy.
4. US3 (P3) → Update Venue + Sections. Validar, demo/deploy.
5. US4 (P4) → Exclusão de Venue com RESTRICT. Validar, demo/deploy.
6. US5 (P4) → Exclusão individual de Seção. Validar, demo/deploy.
7. Polish (Phase 8) → rodar T053-T058 + quickstart.md completo.
8. Cada história adiciona valor e NÃO quebra as anteriores.

### Parallel Team Strategy (multiple devs)

1. Time conclui Phase 1 (Setup) juntos — rápido.
2. Phase 2: Foundational tasks paralelos (T004 a T014 em paralelo por responsabilidade).
3. Foundational concluído:
   - Dev A + Dev B: User Story 1 (P1) priority. A faz domain/tests (T015-16), B faz integration/contract/E2E tests (T017-19). Depois ambos implementam (T020-26).
   - Dev C (opcional): User Story 2 (P2) em paralelo. Tests + implementation de Section.
4. US1 + US2 → US3 (depende de ambos resources). Dev A e B juntos US3.
5. US4 e US5 em paralelo por Dev C e Dev A/B quando livres.
6. Todos participam do Polish (Phase 8).

---

## Notes

- [P] tasks = diferentes arquivos, sem dependências de escrita ou de leitura de dado incompleto
- [Story] label mapeia task para user story específico para rastreabilidade
- Cada user story é independentemente completável e testável (ver "Independent Test" em cada fase)
- Garantir testes FALHAM antes de implementar (TDD) e PASSAM antes de merge
- Commit após cada task ou grupo lógico (ex.: após T001-T003 = Setup)
- Parar em cada checkpoint para validar a story independentemente
- Evitar: tasks vagas, conflitos no mesmo arquivo, cross-story dependencies que quebram independência
- Riscos conhecidos (ver spec.md "Riscos"): sem evento SectionDeleted/VenueDeleted para inventory microservice; validação de capacidade vs tickets vendidos fora de escopo deste serviço. NÃO adicionar código para esses pontos nesta feature.
