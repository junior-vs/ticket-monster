# Implementation Plan: US-CAT-10 - Gerenciar Venue e Section

**Branch**: `010-gerenciar-locais-secoes` | **Date**: 2026-08-06 | **Spec**: `microservice-catalog/specs/010-gerenciar-locais-secoes/spec.md`

**Input**: Feature specification from `microservice-catalog/specs/010-gerenciar-locais-secoes/spec.md`

## Summary

Implementar CRUD administrativo e leitura pública para `Venue` e `Section` no `microservice-catalog`, preservando RN07, RN11 e RN12 diretamente nas invariantes de domínio e nas constraints PostgreSQL já aplicadas. `Section.capacity` permanece somente leitura e derivada pelo banco via coluna gerada; payloads de escrita não aceitam nem propagam `capacity`.

A implementação **segue estritamente a convenção arquitetural já estabelecida e em produção pela feature 009 (`event-categories`)**: pacotes planos por camada dentro de `br.vsjr.labs.ticketmonster.catalog` (não pacotes por agregado). `VenueEntity`, `SectionEntity` e `VenueAddressEmbeddable` **já existem** em `catalog.domain.entity` e são reaproveitados sem alteração estrutural.

## Technical Context

**Language/Version**: Java 25 conforme `microservice-catalog/pom.xml` (`maven.compiler.release=25`). A constituição define Java 21 como baseline mínimo; Java 25 é uma versão acima do baseline, já em uso pela feature 009 — mantida por consistência, não é uma decisão nova desta feature.

**Primary Dependencies**: Quarkus 3.37.4; `quarkus-rest`, `quarkus-rest-jackson`, `quarkus-hibernate-reactive-panache`, `quarkus-reactive-pg-client`, `quarkus-jdbc-postgresql`, `quarkus-liquibase`, `quarkus-redis-client`, `quarkus-messaging-kafka`, `quarkus-oidc`, `quarkus-opentelemetry`, `quarkus-micrometer-registry-prometheus`, `quarkus-smallrye-health`, `quarkus-smallrye-fault-tolerance`. MapStruct (`org.mapstruct`, `componentModel = "cdi"`) já configurado via `QuarkusMappingConfig` — reaproveitado, não introduzido.

**Storage**: PostgreSQL `catalog_db`, schema `catalog`. O schema de `venue`/`section` **já está aplicado** por `db/migration/V1.0__catalog_schema.sql` via Liquibase (`db.changelog-master.xml`) — nenhuma nova migração é necessária para esta feature. Redis para cache-aside, reaproveitando `catalog.cache.key-prefix` já configurado em `application.properties`. Kafka topic `catalog-events` já configurado como *outgoing channel*, mas sem contrato aprovado para eventos de exclusão/alteração de Section relevantes ao `microservice-inventory`.

**Testing**: Maven com Quarkus JUnit e REST Assured. Unitários de domínio/casos de uso, contratos REST, integração com Testcontainers PostgreSQL/Redis para fluxos afetados, E2E do fluxo P1 de criação de Venue.

**Target Platform**: Microsserviço backend Quarkus executando em container Linux/Kubernetes ou local via Docker Compose compartilhado.

**Project Type**: Web service REST reativo, camadas planas por responsabilidade (não hexagonal por agregado) dentro de `microservice-catalog`.

**Performance Goals**: Leituras públicas de catálogo p95 <= 250 ms em cache quente; escritas administrativas p95 <= 200 ms; erro 5xx em steady-state < 0,1% para esta feature.

**Constraints**: Erros via RFC 7807; rotas administrativas exigem `ROLE_ADMIN`; paginação pública/admin usa `page`/`size` base 0 (reaproveitar `catalog.domain.vo.PageRequest`/`PageResult` já existentes); domínio não depende de HTTP, ORM, Kafka ou Redis; sem chamada síncrona para `microservice-inventory`; `capacity` nunca é entrada de escrita.

**Scale/Scope**: CRUD de `Venue` e `Section`, leitura de lista/detalhe, invalidação síncrona de cache, contratos REST v1, hooks de observabilidade.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Layer boundaries**: PASS. Estrutura reaproveita exatamente a separação já usada por `event-categories`: `domain.model` (VOs puros) / `domain.entity` (Panache) / `application.port.{in,out}` / `application.usecase` / `application.mapper` / `adapter.in.dto` / `adapter.out.dto` / `adapter.rest` / `adapter.rest.mapper`. Não introduz pacote por agregado (`catalog.venue.*`), corrigindo a divergência da versão anterior deste plano.
- **Contract-first scope**: PASS WITH RISK. Contrato REST v1 deve ser corrigido para refletir **dois resources por entidade** (público sem prefixo + admin com prefixo `/admin`), como já ocorre em produção para `event-categories` (`/api/v1/event-categories` vs `/api/v1/admin/event-categories`) — a versão anterior do `contracts/openapi.yaml` desta feature definia um único path `/venues` incompatível com esse padrão e com a própria descrição de tarefas. Eventos `SectionDeleted`, `SectionUpdated` (capacidade) e `VenueDeleted` continuam como dependência arquitetural não aprovada; esta feature não deve publicar evento novo sem ADR/contrato complementar.
- **Test depth**: PASS. Unitários, contrato REST, integração Testcontainers e E2E P1 previstos em `tasks.md`.
- **UX consistency**: PASS. RFC 7807, 401/403 para auth, 404 para ausentes, 409 para unicidade/restrição relacional, paginação base 0, UUIDs em todos os recursos.
- **Performance budgets**: PASS. Orçamentos validados via métricas Prometheus e testes de integração/carga local.

Post-design re-check: PASS WITH TRACKED RISK. Os artefatos de fase 1 mantêm o gap de eventos para inventory como risco explícito e não introduzem acoplamento síncrono cross-service.

## Riscos e Pré-condições de Build (não introduzidos por esta feature, mas bloqueantes)

- `EventCategoryExceptionMapper` importa `catalog.adapter.out.dto.ProblemDetails`, que **não existe em nenhum lugar do repositório**. Esta feature depende do mesmo tipo (RFC 7807 compartilhado) para `VenueExceptionMapper`/`SectionExceptionMapper` — a criação de `ProblemDetails` está incluída em `tasks.md` desta feature (T0xx), pois é pré-requisito comum e atualmente ausente.
- `EventCategoryService` implementa contra `catalog.application.port.out.EventCategoryRepositoryPort` e `EventCategoryCachePort`, que também **não existem no repositório** — o módulo `microservice-catalog` não compila no `main` atual por causa da feature 009, independentemente desta feature. Isso é um gap de outra feature (categorias de evento), fora do escopo de correção aqui. **Recomendação**: abrir ticket específico para `event-categories` antes ou em paralelo à execução desta feature, para que o CI volte a passar.

## Project Structure

### Documentation (this feature)

```text
microservice-catalog/specs/010-gerenciar-locais-secoes/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── contracts/
│   └── openapi.yaml   # requer atualização: dois resources por entidade (ver Constitution Check)
├── quickstart.md
└── tasks.md
```

### Source Code (repository root) — reaproveita a convenção real, plana por camada

```text
microservice-catalog/
├── pom.xml
├── src/main/java/br/vsjr/labs/ticketmonster/catalog/
│   ├── domain/
│   │   ├── model/          # Venue, VenueId, VenueName, Address, Section, SectionId, SectionName (NOVOS)
│   │   ├── entity/          # VenueEntity, SectionEntity, VenueAddressEmbeddable (JÁ EXISTEM — reaproveitados)
│   │   ├── exception/       # VenueAlreadyExistsException, VenueNotFoundException, VenueHasShowsException,
│   │   │                    # SectionAlreadyExistsException, SectionNotFoundException, InvalidSectionInputException (NOVOS)
│   │   └── vo/               # PageRequest, PageResult (JÁ EXISTEM — reaproveitados)
│   ├── application/
│   │   ├── port/in/         # CreateVenueUseCase, UpdateVenueUseCase, DeleteVenueUseCase, GetVenueUseCase,
│   │   │                    # ListVenuesUseCase, AddSectionUseCase, UpdateSectionUseCase, DeleteSectionUseCase,
│   │   │                    # GetSectionUseCase, ListSectionsUseCase (NOVOS)
│   │   ├── port/out/        # VenueRepositoryPort, VenueCachePort, SectionRepositoryPort (NOVOS)
│   │   ├── usecase/         # VenueService, SectionService (NOVOS)
│   │   └── mapper/           # VenueMapper, SectionMapper (NOVOS)
│   └── adapter/
│       ├── in/dto/           # CreateVenueRequest, UpdateVenueRequest, CreateSectionRequest, UpdateSectionRequest (NOVOS)
│       ├── out/dto/          # VenueResponse, SectionResponse, VenuePageResponse, SectionPageResponse,
│       │                    # ProblemDetails (NOVO — pré-requisito compartilhado, ver Riscos)
│       ├── out/persistence/  # PanacheVenueRepository, PanacheSectionRepository (NOVOS — extensão consistente de adapter.out.*)
│       ├── out/cache/        # RedisVenueCache (NOVO — extensão consistente de adapter.out.*)
│       ├── rest/             # VenueResource, VenueAdminResource, SectionResource, SectionAdminResource (NOVOS)
│       └── rest/mapper/      # VenueExceptionMapper (NOVO)
└── src/test/java/br/vsjr/labs/ticketmonster/catalog/
    ├── unit/domain/
    ├── rest/
    └── integration/
```

**Structure Decision**: Reaproveitar integralmente a árvore de pacotes plana já em produção (`catalog.domain.*`, `catalog.application.*`, `catalog.adapter.*`), sem sub-pacote por agregado (`catalog.venue.*`). `adapter.out.persistence` e `adapter.out.cache` são extensões novas, mas consistentes com o precedente `adapter.out.dto` já existente — não uma convenção paralela.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Event propagation gap for inventory | Inventory snapshots dependem de eventos de catálogo, mas `SectionDeleted`/`VenueDeleted` e semântica de redução de capacidade não estão especificados | Publicar eventos ad hoc violaria disciplina contract-first e poderia quebrar consumidores |
| Correção de contrato REST (dois resources vs. path único) | O `openapi.yaml` anterior definia `/venues` único; a implementação real de referência (`event-categories`) usa dois resources com prefixo `/admin` | Manter path único criaria uma segunda convenção de rota no mesmo serviço, quebrando consistência de UX (Princípio IV) |