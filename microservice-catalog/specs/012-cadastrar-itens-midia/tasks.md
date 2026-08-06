# Tasks: Cadastrar Itens de Mídia com Validação e Fallback (Admin)

**Input**: Design documents from `microservice-catalog/specs/012-cadastrar-itens-midia/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/openapi.yaml, quickstart.md

**Tests**: Testes são OBRIGATÓRIOS por Constituição III (E2E P1) e por FR-006 (unitários de domínio, contrato REST, integração Testcontainers Postgres/Redis, E2E P1 com WireMock de servidor remoto).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Project root: `microservice-catalog/` (Maven module)
- Sources: `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/`
- Tests: `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/media/`
- Resources: `microservice-catalog/src/main/resources/application.properties`; `db/migration/V1__init.sql` já contém DDL de `media_type_catalog`, `media_item` e FK `event.media_item_id ... ON DELETE SET NULL` — NÃO criar nova migração.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Pacotes e properties de configuração do agregado MediaItem/MediaTypeCatalog. O projeto Quarkus já existe; Nenhuma nova migração é necessária.

- [ ] T001 Criar estrutura de pacotes do agregado `catalog.media.*` (domain, application, adapter/in/rest/dto, adapter/in/rest/exception, adapter/out/persistence/entity, adapter/out/cache, adapter/out/urlcheck) em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/`
- [ ] T002 [P] Criar estrutura de pacotes de testes do agregado `catalog.media.*` (domain/, application/, adapter/in/rest/, adapter/out/persistence/, adapter/out/cache/, adapter/out/urlcheck/) em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/media/`
- [ ] T003 [P] Adicionar propriedades de configuração do MediaItem em `microservice-catalog/src/main/resources/application.properties`:
  - `catalog.media.url-check.timeout-ms=250` (timeout check remoto URL reativo)
  - `catalog.media.fallback-file=not_available.jpg` (chave lógica de fallback padrão)
  - `catalog.media.cache.ttl-seconds=3600` (TTL cache Redis individual)
  - (opcional) `catalog.media.cache.url-prefix=` (prefixo URL público para cachedFileName resolução CDN/S3 fora de escopo desta feature)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Domínio puro (MediaItem, MediaTypeCatalog SEM framework), portas de saída (repositórios, cache, validador de URL remoto), Panache entities JPA mapping, implementações adapter de persistência e cache. Bloqueia todas as user stories.

**⚠️ CRITICAL**: Nenhuma US pode começar sem: domínio puro + portas + Panache entities + adapter repos + adapter cache + adapter URL check.

- [ ] T004 [P] Criar entidade de DOMÍNIO PURO `MediaTypeCatalog` (tabela de domínio, SEM annotations JPA/ORM) em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/domain/MediaTypeCatalog.java`: campos `code String (PK, ex. "IMAGE")`, `description String`, `enabled boolean`. Construtor validando code not blank.
- [ ] T005 [P] Criar entidade de DOMÍNIO PURO `MediaItem` (aggregate root, SEM annotations JPA/ORM) em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/domain/MediaItem.java`: campos `id UUID`, `mediaTypeCode String (FK p/ MediaTypeCatalog)`, `url String (2048)`, `cachedFileName String (nullable, 255)`, `fallbackApplied boolean`, `createdAt OffsetDateTime`. Invariantes: `mediaTypeCode` not blank; `url` not blank; `fallbackApplied` default false se não informado.
- [ ] T006 [P] Criar `UrlValidator` utilitário de DOMÍNIO PURO (sem dependências) em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/domain/UrlValidator.java`:
  - `static boolean isValidScheme(String url)`: valida `^https?://` case-insensitive; retorna false para `ftp://`, vazio, etc.
  - `static boolean isWellFormed(String url)`: valida que URL parseia via `java.net.URI` sem lançar (formato geral, não null, length ≤ 2048).
- [ ] T007 [P] Criar fábrica estática `MediaItemFactory` de DOMÍNIO PURO em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/domain/MediaItemFactory.java`:
  - `static MediaItem createNew(String mediaTypeCode, String url, boolean remoteAccessible, String fallbackFileName)`: se `remoteAccessible = true` → `fallbackApplied=false`, `cachedFileName=null` (ou opcional cache se existir); se `false` → `fallbackApplied=true`, `cachedFileName=fallbackFileName`. SEM annotations, SEM I/O, SEM framework.
- [ ] T008 [P] Criar porta de saída `MediaItemRepositoryPort` (interface SEM dependências) em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/domain/MediaItemRepositoryPort.java`: métodos `Uni<MediaItem> save(MediaItem item)`, `Uni<MediaItem> findById(UUID id)`, `Uni<Page<MediaItem>> findAll(int page, int size)`, `Uni<Boolean> existsByUrl(String url, UUID excludeIdIfAny)`, `Uni<Void> deleteById(UUID id)`.
- [ ] T009 [P] Criar porta de saída `MediaTypeCatalogReaderPort` (interface SEM dependências) em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/domain/MediaTypeCatalogReaderPort.java`: métodos `Uni<Optional<MediaTypeCatalog>> findByCodeAndEnabled(String code)` (retorna vazio se desabilitado ou inexistente).
- [ ] T010 [P] Criar porta de saída `MediaItemCachePort` (interface SEM dependências Redis) em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/domain/MediaItemCachePort.java`: `Uni<MediaItem> get(UUID id)`, `Uni<Void> set(MediaItem item, Duration ttl)`, `Uni<Void> evict(UUID id)`.
- [ ] T011 [P] Criar porta de saída `RemoteUrlValidatorPort` (interface SEM dependências Vert.x/RestClient) em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/domain/RemoteUrlValidatorPort.java`: `Uni<Boolean> isAccessible(String url)` — retorna `true` se HEAD/GET remoto respondeu OK (<400); retorna `false` SEQUER se qualquer exceção/timeout/erro (4xx/5xx/network/timeout). **NÃO** lançar exceção p/ qualquer falha de rede.
- [ ] T012 [P] Criar Panache entity `MediaTypeCatalogEntity` em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/adapter/out/persistence/entity/MediaTypeCatalogEntity.java`: mapear `catalog.media_type_catalog`, colunas `code VARCHAR(30) PK`, `description VARCHAR(120) NOT NULL`, `enabled BOOLEAN NOT NULL DEFAULT TRUE`. Usar Hibernate Reactive Panache.
- [ ] T013 [P] Criar Panache entity `MediaItemEntity` em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/adapter/out/persistence/entity/MediaItemEntity.java`: mapear `catalog.media_item`, colunas `id UUID PK`, `media_type_code VARCHAR(30) FK catalog.media_type_catalog.code`, `url VARCHAR(2048) NOT NULL`, `cached_file_name VARCHAR(255) NULL`, `fallback_applied BOOLEAN NOT NULL DEFAULT FALSE`, `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`. Unique constraint `uq_media_item_url` em `url`. Many-to-One `mediaTypeCatalog` ou apenas coluna `mediaTypeCode` (simplificar já que MediaTypeCatalog é tabela de domínio read-only para esta feature).
- [ ] T014 Implementar adapter persistência `PanacheMediaItemRepository` implementa `MediaItemRepositoryPort` em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/adapter/out/persistence/PanacheMediaItemRepository.java`: mapper MediaItem ↔ MediaItemEntity. `existsByUrl`: native query `SELECT 1 FROM catalog.media_item WHERE url = $1 [AND id <> $2]` case-sensitive ou via Panache. Capturar `PersistenceException` ou `ConstraintViolationException` de `uq_media_item_url` e relançar como exceção de domínio `DuplicateMediaUrlException` (ou deixar exceção para mapper REST tratar).
- [ ] T015 Implementar adapter persistência `PanacheMediaTypeCatalogReader` implementa `MediaTypeCatalogReaderPort` em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/adapter/out/persistence/PanacheMediaTypeCatalogReader.java`: findById Panache + filtro `enabled = TRUE`.
- [ ] T016 Implementar adapter cache `RedisMediaItemCache` implementa `MediaItemCachePort` em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/adapter/out/cache/RedisMediaItemCache.java`: chave `catalog:media-item:{id}` (FR-007). Serializar `MediaItem` para JSON via `ObjectMapper`; desserializar de volta. TTL via `@ConfigProperty(name = "catalog.media.cache.ttl-seconds") Duration`. Usar `ReactiveRedisDataSource` Quarkus ou `RedisClient` reativo.
- [ ] T017 Implementar adapter validador remoto `VertxRemoteUrlValidator` implementa `RemoteUrlValidatorPort` em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/adapter/out/urlcheck/VertxRemoteUrlValidator.java`:
  - Usar Vert.x `WebClient` injetado (nativo reativo, já transitivo na stack Quarkus REST HTTP).
  - Timeout: `.timeout(configProperty catalog.media.url-check.timeout-ms)` (default 250 ms).
  - Requisição HTTP HEAD primeiro; se HEAD falhar por método não suportado, fallback GET com `Range: bytes=0-0` (evitar download gigante).
  - `onFailure().recoverWithItem(false)` → **QUALQUER** exceção (ConnectException, TimeoutException, HTTP 4xx/5xx, SSL error, etc.) retorna `false`. Apenas status < 400 retorna `true`.
  - O adapter NÃO faz bloqueio de thread; retorna `Uni<Boolean>` reativo.

**Checkpoint**: Foundation ready. Domínio puro sem framework, portas, panache entities, repositórios Panache, cache Redis, Vert.x WebClient URL check. User story implementation can now begin.

---

## Phase 3: User Story 1 - Cadastrar Novo Item de Mídia com Validação e Fallback (Priority: P1) 🎯 MVP

**Goal**: Admin ROLE_ADMIN cria MediaItem com URL + mediaTypeCode. Sistema: (1) valida URL esquema http(s) e formato (400 se inválido); (2) valida mediaType existe habilitado (400 se não); (3) check prévio unicidade URL (409 se duplicado); (4) check remoto URL NÃO BLOQUEANTE com timeout 250 ms — se sucesso → `fallbackApplied=false`, se QUALQUER falha/timeout → `fallbackApplied=true` e `cachedFileName=not_available.jpg`. Resultado sempre HTTP 201 Created com dados completos e UUID. Cache-aside individual Redis preenchido lazy no primeiro GET público.

**Independent Test**: Cenários de quickstart.md US1: (Cenário 1) POST URL acessível → 201 `fallbackApplied: false`; POST mesma URL de novo → 409. (Cenário 2a) POST `https://httpstat.us/404` → 201 `fallbackApplied: true, cachedFileName:"not_available.jpg"`. (Cenário 2b) POST `https://httpstat.us/200?sleep=2000` (timeout 2s > 250ms) → 201 `fallbackApplied: true`. (Cenário 3a) `ftp://files.local/x.jpg` → 400 `Esquema de URL inválido`. RFC 7807 em todos os erros; 401 sem token; 403 sem ROLE_ADMIN. E2E P1 usa WireMock para mockar servidor HTTP remoto.

### Tests for User Story 1 (REQUIRED by FR-006) ⚠️

> **NOTE: Escrever testes PRIMEIRO, garantir que FALHEM antes da implementação.**

- [ ] T018 [P] [US1] Teste unitário de domínio: `UrlValidator.isValidScheme` aceita `http://example.com`, `https://example.com:8443/x?y`; rejeita `ftp://x`, `httpx://x`, `http`, vazio, null — em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/media/domain/UrlValidatorTest.java`
- [ ] T019 [P] [US1] Teste unitário de domínio: `MediaItemFactory.createNew` quando `remoteAccessible=false` → `fallbackApplied=true`, `cachedFileName=fallbackFileName`; quando `remoteAccessible=true` → `fallbackApplied=false`, `cachedFileName=null` (ou opcional vazio) — em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/media/domain/MediaItemFactoryFallbackTest.java`
- [ ] T020 [P] [US1] Teste unitário de domínio/invariante: `MediaItem` rejeita `mediaTypeCode` nulo/branco e `url` nulo/branco (lança exceção de domínio) — em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/media/domain/MediaItemInvariantsTest.java`
- [ ] T021 [P] [US1] Teste de contrato REST `MediaItemAdminResourceContractTest`: (a) POST admin `/api/v1/admin/media-items` com URL acessível (mock RemoteUrlValidatorPort via `@InjectMock` retornando `Uni.createFrom().item(true)`) → 201 e `fallbackApplied=false`; (b) mesmo payload novamente → 409 `title="URL já cadastrada"`; (c) URL `ftp://` → 400 `title="Esquema de URL inválido"`; (d) sem token → 401; (e) token sem ROLE_ADMIN → 403. Usar `@QuarkusTest`, REST Assured, validar Problem RFC 7807 headers `application/problem+json` — em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/media/adapter/in/rest/MediaItemAdminResourceContractTest.java`
- [ ] T022 [P] [US1] Teste unitário adapter `VertxRemoteUrlValidator` com mock Vert.x WebClient: (1) 200 OK → true; (2) 404 → false; (3) 500 → false; (4) timeout após 250ms → false; (5) ConnectException/network error → false. O adapter NUNCA deve lançar exceção p/ rede. Em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/media/adapter/out/urlcheck/VertxRemoteUrlValidatorUnitTest.java`
- [ ] T023 [P] [US1] Teste integração Testcontainers PostgreSQL: constraint `ck_media_item_url_scheme` (CHECK `url ~* '^https?://'`) é lastro de banco — inserir URL `ftp://` bypassando validador de domínio (via Panache native query direto) deve disparar erro de constraint. Constraint `uq_media_item_url`: duas threads criando mesma URL concorrentemente → 409 na segunda. Em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/media/adapter/out/persistence/MediaItemConstraintsIT.java`
- [ ] T024 [P] [US1] Teste integração Testcontainers Redis: cache-aside `catalog:media-item:{id}` — primeiro GET público miss → banco → cache set → segundo GET público vem do cache (validar ttl ou count de chamadas repo com mockito spy). Após delete → cache evicted. Em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/media/adapter/out/cache/RedisMediaItemCacheIT.java`
- [ ] T025 [P] [US1] Teste E2E P1 (obrigatório Constituição III): Quarkus + Postgres + Redis via Testcontainers + WireMock (Quarkus WireMock support ou `@TestResource` WireMockServer standalone). 3 cenários WireMock: (a) rota `/ok.jpg` 200 → POST retorna 201 `fallbackApplied=false`; (b) rota `/slow.jpg` `withFixedDelay(2000)` → timeout 250ms → 201 `fallbackApplied=true, cachedFileName="not_available.jpg"`; (c) rota `/notfound.jpg` 404 → 201 `fallbackApplied=true`. Validar response body completo e RFC 7807 para erros de validação. Em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/media/MediaItemE2EP1Test.java`

### Implementation for User Story 1

- [ ] T026 [P] [US1] Criar DTOs REST em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/adapter/in/rest/dto/`:
  - `CreateMediaItemRequest`: apenas `mediaTypeCode` (obrigatório 1..30 chars) + `url` (obrigatório, max 2048, format uri pattern `^https?://.*`). **SEM `fallbackApplied` nem `cachedFileName` na entrada**.
  - `MediaItemResponse`: `id UUID`, `mediaTypeCode`, `url`, `cachedFileName nullable`, `fallbackApplied boolean`, `createdAt OffsetDateTime`.
  - `MediaItemPageResponse`: `content List<MediaItemResponse>`, `page int`, `size int`, `totalElements long`.
- [ ] T027 [P] [US1] Implementar caso de uso `CreateMediaItemUseCase` em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/application/CreateMediaItemUseCase.java`:
  - Passos em pipeline reativo `Uni<MediaItem>`:
    1. `UrlValidator.isValidScheme(url)` + `isWellFormed(url)` → senão lança `InvalidMediaUrlSchemeException` (400) ou `MalformedUrlException` (400).
    2. `mediaTypeCatalogReader.findByCodeAndEnabled(mediaTypeCode)` mapa para Optional → vazio lança `InvalidMediaTypeException` (400 "Tipo de mídia inválido ou inativo").
    3. `mediaItemRepository.existsByUrl(url, null)` → true lança `DuplicateMediaUrlException` (409 "URL já cadastrada").
    4. `remoteUrlValidator.isAccessible(url)` → boolean `accessible`. **Qualquer falha no Uni é recuperada para false**.
    5. `MediaItemFactory.createNew(mediaTypeCode, url, accessible, fallbackFileConfig)` → `MediaItem` novo.
    6. `mediaItemRepository.save(item)`.
    7. (Opcional) `mediaItemCache.set(savedItem, ttl)` ou deixar lazy no Get.
  - Injeta: 4 portas + `@ConfigProperty(name="catalog.media.fallback-file") String`.
  - **NÃO** depende de REST/JPA annotations, Quarkus classes ou Redis diretamente.
- [ ] T028 [P] [US1] Implementar caso de uso `GetMediaItemUseCase` em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/application/GetMediaItemUseCase.java`:
  - Cache-aside: primeiro `cache.get(id)`; se miss → `repo.findById(id)`; se hit DB → `cache.set(item, ttl)`; retorna item ou lança `MediaItemNotFoundException` (404).
- [ ] T029 [P] [US1] Implementar caso de uso `ListMediaItemsUseCase` em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/application/ListMediaItemsUseCase.java`: paginação `page` base 0, `size` (default 20, validação 1..100). Apenas `repo.findAll(page, size)`; listagens NÃO são cacheadas nesta feature.
- [ ] T030 [US1] Implementar exceções de domínio e ExceptionMapper RFC 7807 em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/adapter/in/rest/exception/MediaItemExceptionMapper.java`:
  - Exceções de domínio a criar em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/domain/exception/`:
    - `InvalidMediaUrlSchemeException`: 400 title `"Esquema de URL inválido"`, detail `"Apenas http:// e https:// são aceitos."`
    - `MalformedUrlException`: 400 title `"URL malformada"`
    - `InvalidMediaTypeException`: 400 title `"Tipo de mídia inválido ou inativo"`
    - `DuplicateMediaUrlException`: 409 title `"URL já cadastrada"`
    - `MediaItemNotFoundException`: 404 title `"Recurso não encontrado"`
  - Mapper: `@Provider` `@Produces(MediaType.APPLICATION_PROBLEM_JSON)`. Também captura `ConstraintViolationException` Hibernate/JPA: se mensagem contém `uq_media_item_url` → 409 duplicate; se contém `ck_media_item_url_scheme` → 400 esquema inválido. Também validação JAX-RS `@Valid` / `ConstraintViolation` de request → 400 `title="Validation failed"` com `instance` path.
- [ ] T031 [US1] Implementar `MediaItemAdminResource` (admin ROLE_ADMIN) em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/adapter/in/rest/MediaItemAdminResource.java`:
  - `@Path("/api/v1/admin/media-items")`, `@RolesAllowed("ROLE_ADMIN")` na classe.
  - `@POST @Consumes(JSON) @Produces(JSON)`: aceita `CreateMediaItemRequest`, usa `CreateMediaItemUseCase` → 201 Created com `Location: /api/v1/admin/media-items/{id}` header + body `MediaItemResponse`.
  - `@GET @Path("/{id}")`: 200 `MediaItemResponse` ou 404 Problem via `GetMediaItemUseCase`.
  - `@GET`: 200 `MediaItemPageResponse` via `ListMediaItemsUseCase` (page=0, size=20 padrão).
  - `@DELETE @Path("/{id}")`: T050 US3 (implementação na fase 5).
  - Mapeamento DTO ↔ MediaItem dentro do resource ou mapper package-private.
- [ ] T032 [US1] Implementar `MediaItemPublicResource` (público SEM auth) em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/adapter/in/rest/MediaItemPublicResource.java`:
  - `@Path("/api/v1/media-items")`, sem `@RolesAllowed` (aberto ao catálogo público).
  - `@GET @Path("/{id}")`: 200 `MediaItemResponse` ou 404 Problem via `GetMediaItemUseCase` (usa cache-aside).
  - `@GET`: 200 `MediaItemPageResponse` via `ListMediaItemsUseCase`.

**Checkpoint**: US1 funcional e independentemente testável. Criar itens de mídia IMAGE com fallback automático, erros RFC 7807, cache Redis individual, auth admin.

---

## Phase 4: User Story 2 - Suportar Catálogo Extensível de Tipos de Mídia (Vídeo/Áudio) (Priority: P2)

**Goal**: Criar MediaItem com `mediaTypeCode` qualquer (ex.: `VIDEO`, `AUDIO`) que exista em `media_type_catalog` e `enabled=true`. Validação de tipo é extensível via seed/migração SEM redeploy do serviço. Tipos inexistentes ou desabilitados → 400.

**Independent Test**: (1) inserir via SQL/migration nova linha em `media_type_catalog` (VIDEO, "Video promocional", TRUE); POST admin com `mediaTypeCode="VIDEO"` → 201 Created; response contém `mediaTypeCode=VIDEO`. (2) POST `mediaTypeCode="DOCX"` (não existe) → 400 `Tipo de mídia inválido ou inativo`. (3) desabilitar IMAGE: `UPDATE catalog.media_type_catalog SET enabled = FALSE WHERE code = 'IMAGE'`; tentar POST novo IMAGE → 400. Teste unitário confirma que não há enum Java fechado (todos os tipos são dinâmicos via tabela).

### Tests for User Story 2 (REQUIRED)

- [ ] T033 [P] [US2] Teste contrato REST extensibilidade: inserir `VIDEO` via Panache `@TestTransaction` (Testcontainers Postgres); POST MediaItem com `mediaTypeCode=VIDEO` → 201; POST `DOCX` → 400; desabilitar `IMAGE` e tentar POST IMAGE → 400. Em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/media/adapter/in/rest/MediaTypeExtensibilityContractTest.java`
- [ ] T034 [P] [US2] Teste unitário `PanacheMediaTypeCatalogReader`: consulta retorna Optional vazio quando `enabled=false` ou código inexistente; Optional presente quando enabled. Em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/media/adapter/out/persistence/MediaTypeCatalogReaderTest.java`

### Implementation for User Story 2

> OBSERVAÇÃO: A maior parte da lógica já foi implementada em US1 (T027 usa `mediaTypeCatalogReader.findByCodeAndEnabled`). Esta US garante o CONTRATO de que tipos além de IMAGE são suportados SEM código enum fechado. Validação extra de compatibilidade com Video/Audio (ex.: check de headers Content-Type) é fora de escopo (apenas URL schema + acessibilidade).

- [ ] T035 [P] [US2] Garantir que NÃO existe `enum MediaType { IMAGE, VIDEO, AUDIO }` no código. Em vez disso, confirmar uso de `String mediaTypeCode` em:
  - `MediaItem.domain` (T005 já está String)
  - `MediaItemEntity.persistence` (T013 deve ser String, não @Enumerated)
  - `CreateMediaItemRequest` (String)
  - Se houver qualquer `@Enumerated(EnumType.STRING)` ou Java Enum em `MediaTypeCatalog.code` / `MediaItemEntity.mediaTypeCode`, refatorar para `String`. Esta tarefa garante RN34 `[ALTERA RN34]` sem redeploy.
  - Nota: Se já estiver tudo String em US1 → task é apenas validação (adicionar comentário ou teste unitário que faça reflexão para garantir sem enum).
- [ ] T036 [US2] Seed inicial de dados mínimos em `V1__init.sql` já contém `INSERT INTO catalog.media_type_catalog VALUES ('IMAGE', 'Imagem promocional', TRUE);`. Adicionar comentário inline no PanacheMediaTypeCatalogReader ou no application.properties documentando como adicionar novos tipos: "Para adicionar VIDEO/AUDIO, inserir linha no media_type_catalog via Liquibase changelog / migração; nenhum redeploy é necessário se já estiver String". (Sem criar API de escrita nesta feature — FR-004).

**Checkpoint**: US2 pronta. Extensibilidade de tipos de mídia garantida. A equipe de dados pode adicionar tipos por migração sem alterar código Java.

---

## Phase 5: User Story 3 - Consultar e Remover Itens de Mídia (Priority: P3)

**Goal**: Admin lista paginada itens de mídia (GET admin; já em parte feito em T031). Admin exclui MediaItem por ID. Exclusão SEMPRE permitida mesmo com eventos vinculados. DDL `event.media_item_id ... ON DELETE SET NULL` automaticamente zera a FK nos eventos — a aplicação NÃO deve bloquear (nunca 409). Após exclusão, cache Redis `catalog:media-item:{id}` é invalidado síncronamente.

**Independent Test**: (1) Criar MediaItem (POST) + associar a Event (insert via SQL native: `INSERT catalog.event ... media_item_id = $id`); DELETE admin `/api/v1/admin/media-items/{id}` → 204 No Content; SELECT `media_item_id` do evento → NULL (SET NULL aplicado). (2) Confirmar chave Redis do item foi removida (DEL síncrono). (3) MediaItem não existe → DELETE retorna 404 Problem.

### Tests for User Story 3 (REQUIRED)

- [ ] T037 [P] [US3] Teste contrato REST DELETE: (a) MediaItem existente → 204; (b) inexistente → 404; (c) vinculado a Event → 204 (NÃO 409); validar event.media_item_id virou NULL via SQL nativo pós-delete. Em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/media/adapter/in/rest/MediaItemDeleteContractTest.java`
- [ ] T038 [P] [US3] Teste integração Testcontainers PostgreSQL: DELETE MediaItem vinculado a Event → `ON DELETE SET NULL` executado; zero exceptions de FK. Validar que o Postgres, não a aplicação, cuidou da desvinculação automática (nenhum UPDATE manual). Em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/media/adapter/out/persistence/MediaItemDeleteOnDeleteSetNullIT.java`

### Implementation for User Story 3

- [ ] T039 [P] [US3] Implementar caso de uso `DeleteMediaItemUseCase` em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/application/DeleteMediaItemUseCase.java`:
  - Passos: (1) `mediaItemRepository.findById(id)` → lança 404 se não existir (evict mesmo se não existir? Não necessário). (2) capturar `id` do item existente. (3) `mediaItemRepository.deleteById(id)` (Postgres cuidará automaticamente de `ON DELETE SET NULL` em `catalog.event`). NÃO adicionar queries UPDATE para limpar FK — se chegar a rodar, a aplicação está contornando a DDL corretamente definida, gerando riscos de concorrência. (4) `mediaItemCache.evict(id)` síncrono após sucesso da transação de delete. (5) retorna void.
  - NÃO existe validação de "tem vínculo?" (nunca 409 por vinculo).
- [ ] T040 [US3] Preencher `@DELETE @Path("/{id}")` em `MediaItemAdminResource` (T031): injetar `DeleteMediaItemUseCase`; chamar `useCase.delete(id)`; retornar HTTP 204 No Content. Se `MediaItemNotFoundException` → mapper lança 404 conforme T030.

**Checkpoint**: US3 concluída. CRUD completo MediaItem (create/read/list/delete). Exclusão sempre permitida com SET NULL automático. Cache inválido sincronamente.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Cobertura final, compatibilidade de contrato OpenAPI, UX consistency, smoke local, execução do quickstart.md.

- [ ] T041 [P] Unittest edge cases adicionais (se não cobertos): URL com 2049 caracteres → 400; query string `?x=1&y=2` aceita; URL com IP e porta `http://127.0.0.1:8080/x.jpg` aceita; fragmento `#section` removido ou aceito. Em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/media/domain/UrlValidatorEdgeTest.java`
- [ ] T042 [P] Contract compatibility validation: validar que endpoints e schemas em `contracts/openapi.yaml` batem com código real. Mínimo: `CreateMediaItemRequest` NÃO contém `fallbackApplied`/`cachedFileName` (usar `@ServerTest` ou gerar snapshot de schema Jackson via `ObjectMapper` e comparar com `contracts/openapi.yaml` JSON schema). Ou pelo menos validação manual de que `/admin/media-items` POST/DELETE/GET existe e `/media-items` público GET existe. Em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/media/adapter/in/rest/OpenApiContractCompatibilityTest.java`
- [ ] T043 [P] UX consistency: Validação em testes de que TODOS erros retornam `application/problem+json` com campos RFC 7807 obrigatórios (`type`, `title`, `status`, `detail`, `instance`); paginação base 0; 401/403 corretos; UUIDs em todos IDs; response sempre com `fallbackApplied` e `cachedFileName` transparente. Em `microservice-catalog/src/test/java/br/vsjr/labs/ticketmoster/catalog/media/adapter/in/rest/UxConsistencyTest.java`
- [ ] T044 [P] Métricas e observabilidade: (1) adicionar `@Timed(name = "media_item_url_check_duration")` ou `@Counted(name = "media_item_url_check_failures")` no adapter `VertxRemoteUrlValidator`; ou usar Micrometer diretamente. (2) validar `/q/metrics` expõe contadores após chamadas de check. Em `microservice-catalog/src/main/java/br/vsjr/labs/ticketmoster/catalog/media/adapter/out/urlcheck/VertxRemoteUrlValidator.java` e testes associados. Health `/q/health/live` e `/q/health/ready` por padrão Quarkus; se necessário adicionar readiness custom para Redis/Postgres (fora de escopo se já existir healthcheck padrão).
- [ ] T045 Smoke local: Executar `docker compose -f docker-compose.shared.yml up -d` + `.\mvnw.cmd quarkus:dev` em microservice-catalog. Validar 5 cenários de quickstart.md manualmente ou com scripts PowerShell: (Cenário 1) sucesso URL acessível 201; (Cenário 2a) 404 notfound fallback true 201; (Cenário 2b) timeout 2s fallback true 201; (Cenário 3a) ftp:// → 400; (Cenário 3b) DOCX → 400; (Cenário 4) DELETE sem vínculo → 204; DELETE com vínculo → 204 + event.media_item_id = NULL; (Cenário 5) Cache Redis miss → banco → chave existe; delete → chave removida.
- [ ] T046 [P] Rodar bateria completa de testes: `.\mvnw.cmd -pl microservice-catalog test` (ou `.\mvnw.cmd -pl microservice-catalog failsafe:integration-test` se ITs separados por sufixo `*IT.java`). Garantir que todos os testes de T018–T044 passam. Após: **STOP — validar resultado antes de merge**.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sem dependências
- **Foundational (Phase 2)**: Depende de Setup concluído — **BLOQUEIA** todas user stories
- **US1 (Phase 3)**: Depende de Foundational (todas tasks T004–T017 concluídas). **MVP**.
- **US2 (Phase 4)**: Depende de Foundational + US1 parcial (CreateMediaItemUseCase já usa MediaTypeCatalogReader). Pode começar assim que T009 + T015 + T027 estiverem parcialmente completos; para testes independentes pode usar Panache inserts diretos.
- **US3 (Phase 5)**: Depende de Foundational + US1 (MediaItem repos/resources existem). Pode ser feito em paralelo com US2 por outra pessoa.
- **Polish (Phase 6)**: Depende de US1+US2+US3 concluídas (ou US1 se MVP only).

### User Story Dependencies

- **US1 (P1)**: Após Foundational. Nenhuma dependência das outras histórias → **MVP First**.
- **US2 (P2)**: Lógica de validação de tipo já construída em US1 (T027). Esta US é mais "garantia de compatibilidade sem enum" + testes + validação documental (T035, T036). Pode iniciar cedo.
- **US3 (P3)**: Depende de MediaItemResource existir com @DELETE path (criado em T031, preenchido em T040) + DeleteUseCase T039 + cache evict. Precisa de US1's Redis + Postgres + eventos tabela existir.

### Within Each User Story

1. Escrever testes primeiro (TDD) — todos devem falhar inicialmente.
2. DTOs + UseCases (models/services).
3. Exception Mapper + Resources REST adapters.
4. Executar testes do passo 1 — todos passam.
5. Checkpoint: história validável independentemente.

### Parallel Opportunities (tasks [P])

- T001–T003 Setup paralelo por arquivos
- T004–T011 Foundational paralelo: domínio, validadores, portas, fábrica (9 arquivos diferentes, 0 conflitos)
- T012–T013 Panache entities paralelas
- T018–T025: 8 testes US1 em paralelo (arquivos separados)
- T026, T027, T028, T029: DTOs + 3 UseCases paralelos
- T033–T034 testes US2 paralelos
- T037–T038 testes US3 paralelos
- T041–T044 Polish tasks paralelos entre si
- **US2 e US3 podem ser implementados em paralelo** por duas pessoas depois que US1 estiver com o core pronto.

---

## Parallel Example: User Story 1 (MVP)

```text
# TDD — Todos testes em paralelo:
T018: UrlValidatorTest.java
T019: MediaItemFactoryFallbackTest.java
T020: MediaItemInvariantsTest.java
T021: MediaItemAdminResourceContractTest.java
T022: VertxRemoteUrlValidatorUnitTest.java
T023: MediaItemConstraintsIT.java (Testcontainers Postgres)
T024: RedisMediaItemCacheIT.java (Testcontainers Redis)
T025: MediaItemE2EP1Test.java (Testcontainers + WireMock E2E)

# Implementation em paralelo (após testes scaffolded):
T026: CreateMediaItemRequest.java, MediaItemResponse.java, MediaItemPageResponse.java
T027: CreateMediaItemUseCase.java
T028: GetMediaItemUseCase.java
T029: ListMediaItemsUseCase.java
```

## Parallel Example: US2 + US3 (time com 2 devs pós-MVP)

```text
# Dev A: US2 Extensibilidade Tipos
T033: MediaTypeExtensibilityContractTest.java
T034: MediaTypeCatalogReaderTest.java
T035: Validar sem enum (String everywhere)
T036: Documentar seed de tipos

# Dev B: US3 Consultar e Remover
T037: MediaItemDeleteContractTest.java
T038: MediaItemDeleteOnDeleteSetNullIT.java
T039: DeleteMediaItemUseCase.java
T040: Preencher @DELETE em MediaItemAdminResource
```

---

## Implementation Strategy

### MVP First (Only User Story 1 P1)

1. Concluir Phase 1 (Setup).
2. Concluir Phase 2 Foundational: **priorizar apenas os blocos necessários a US1**: T004 MediaTypeCatalog domain (pode simplificar por String direto em vez de entity), T005 MediaItem domain, T006 UrlValidator, T007 MediaItemFactory, T008 MediaItemRepositoryPort, T009 MediaTypeCatalogReaderPort, T010 CachePort, T011 RemoteUrlValidatorPort, T013 MediaItemEntity, T014 Panache repo, T015 MediaTypeCatalogReader Panache, T016 Redis Cache, T017 Vertx URL check. (Pode pular T012 se MediaTypeCatalog não precisar de escrita.)
3. Concluir Phase 3 US1 COMPLETA (T018–T032).
4. **STOP, VALIDAR**: Rodar Cenário 1, 2a, 2b, 3a, 3b de quickstart.md; validar E2E T025 passa; validar métricas.
5. Deploy/Demo MVP: Admin pode cadastrar IMAGE mídias com fallback automático + cache + erros RFC 7807.

### Incremental Delivery

1. Setup + Foundational → Base pronta.
2. US1 (P1) → MVP funcional. Validar, Demo/Deploy.
3. US2 (P2) → Garantir extensibilidade para Vídeo/Áudio (sem enum fechado). Validar, Demo.
4. US3 (P3) → CRUD: listar paginado, excluir item vinculado SET NULL. Validar, Demo.
5. Polish Phase (T041–T046) + quickstart.md completo.
6. Qualquer história entrega valor.

### Parallel Team Strategy (≥2 devs)

1. Setup juntos (rápido).
2. Foundational paralelo: Dev A domínio + portas; Dev B Panache entities + adapters persistência/cache; Dev C adapter URL check + metrics.
3. Após Foundational:
   - Dev A: US1 tests (T018–T022) + implementação UseCases + AdminResource.
   - Dev B: US1 tests IT/E2E (T023–T025) + PublicResource + ExceptionMapper + Polish tasks.
4. US1 concluído:
   - Dev A: US2 (T033–T036).
   - Dev B: US3 (T037–T040).
5. Polish juntos (T041–T046).

---

## Notes

- [P] tasks = diferentes arquivos, sem dependências
- [Story] label para rastreabilidade por US
- Cada user story independentemente completável e testável
- **BLOQUEIO arquitetural CRÍTICO**: NÃO reintroduzir I/O bloqueante. O check de URL remota DEVE usar Vert.x WebClient reativo (`Uni`) — nunca `HttpClient` síncrono Java, nunca `RestClient` bloqueante.
- `cachedFileName` é chave lógica/identificador de objeto — não gravar caminho absoluto de disco. Resolução CDN/S3 é infra futura.
- Riscos de spec.md: (1) Sem evento Kafka de MediaItemCreated/Deleted nesta feature; se cross-service precisar, é ADR posterior. (2) Backend de armazenamento físico (S3/MinIO) não resolvido nesta feature.
- Nenhuma migração Liquibase adicionada nesta feature: DDL V1__init.sql já contém todas tabelas/constraints necessárias (`media_type_catalog`, `media_item` com unique+check, FK `event.media_item_id ON DELETE SET NULL`).
