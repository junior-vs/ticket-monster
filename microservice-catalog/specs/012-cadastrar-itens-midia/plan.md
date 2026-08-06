# Implementation Plan: US-CAT-12 - Cadastrar Itens de Mídia com Validação e Fallback

**Branch**: `012-cadastrar-itens-midia` | **Date**: 2026-07-29 | **Spec**: `microservice-catalog/specs/012-cadastrar-itens-midia/spec.md`

**Input**: Feature specification from `microservice-catalog/specs/012-cadastrar-itens-midia/spec.md`

## Summary

Implementar CRUD administrativo para `MediaItem` no `microservice-catalog` com validação síncrona não-bloqueante de URL (client HTTP reativo, timeout 250ms), aplicação automática de fallback (`not_available.jpg`) quando a URL remota falha, catálogo extensível de tipos de mídia via `media_type_catalog` (IMAGE/VIDEO/AUDIO), cache-aside Redis para leitura individual e exclusão com desvinculação automática de eventos (`ON DELETE SET NULL`). Validações estruturais (esquema http/https, unicidade de URL) são garantidas por constraints PostgreSQL e por validação de domínio prévia. Erros seguem RFC 7807.

A solução usa Quarkus REST reativo (RESTEasy Reactive + Mutiny), camada de aplicação com casos de uso (`CreateMediaItemUseCase`, `DeleteMediaItemUseCase`, `GetMediaItemUseCase`, `ListMediaItemsUseCase`), domínio sem dependências de framework, persistência via Hibernate Reactive Panache/PostgreSQL, cache-aside Redis (`catalog:media-item:{id}`) e validador de URL remoto baseado em `quarkus-rest-client-reactive` ou `Vert.x WebClient` não-bloqueante.

## Technical Context

**Language/Version**: Java 25 conforme `microservice-catalog/pom.xml` (`maven.compiler.release=25`). A constituição define Java 21 como baseline mínimo; Java 25 é uma versão acima do baseline e deve ser mantida apenas se o runtime alvo suportar esse release.

**Primary Dependencies**: Quarkus 3.37.4; `quarkus-rest`, `quarkus-rest-jackson`, `quarkus-hibernate-reactive-panache`, `quarkus-reactive-pg-client`, `quarkus-jdbc-postgresql`, `quarkus-liquibase`, `quarkus-redis-client`, `quarkus-messaging-kafka`, `quarkus-oidc`, `quarkus-opentelemetry`, `quarkus-micrometer-registry-prometheus`, `quarkus-smallrye-health`, `quarkus-smallrye-fault-tolerance`, `quarkus-rest-client-reactive` (para o HTTP check de URL remota usando `@Timeout`/`@CircuitBreaker` do SmallRye Fault Tolerance) ou `quarkus-vertx-web-client` (Vert.x `WebClient` reativo nativo com timeout configurado).

**Storage**: PostgreSQL `catalog_db`, schema `catalog`, com Liquibase aplicando `db/changelog/db.changelog-master.xml` -> `db/migration/V1__init.sql` que já contém `media_type_catalog` e `media_item` com `event.media_item_id ... ON DELETE SET NULL`; Redis DB 0 para cache-aside individual (`catalog:media-item:{id}`), invalidado de forma síncrona em alteração/exclusão; fallback `not_available.jpg` como asset estático ou chave de object storage (backend S3/MinIO fora de escopo desta feature).

**Testing**: Maven com Quarkus JUnit e REST Assured. Plano de testes: (1) unitários de domínio — validação de esquema de URL, regra de fallback, validação de `media_type_code` habilitado; (2) testes de contrato REST — endpoints admin GET/POST/DELETE; (3) integração com Testcontainers PostgreSQL + Redis para fluxos CRUD completos, incluindo validação de constraint `uq_media_item_url` (HTTP 409) e `ck_media_item_url_scheme`; (4) teste E2E da jornada P1 com mock de servidor HTTP remoto (WireMock ou similar) cobrindo dois cenários: URL remota acessível (`fallback_applied = false`) e URL remota inacessível/timeout (`fallback_applied = true`); (5) teste unitário da exclusão de MediaItem vinculado a Event confirmando `event.media_item_id = NULL` resultante.

**Target Platform**: Microsserviço backend Quarkus executando em container Linux/Kubernetes ou local via Docker Compose compartilhado (`microservice-catalog/docker-compose.yml`).

**Project Type**: Web service REST reativo em arquitetura limpa/hexagonal dentro de `microservice-catalog`.

**Performance Goals**: Escrita administrativa p95 <= 300 ms (proposto por esta spec, sujeito a validação de carga — ver Assumptions); check remoto de URL timeout <= 250 ms sem bloquear thread; erro 5xx em steady-state < 0,1% para esta feature (PR-001).

**Constraints**: Erros via RFC 7807 (FR-005); rotas administrativas POST/DELETE exigem `ROLE_ADMIN`; leitura pública de URLs é sem auth; paginação usa `page` e `size` base 0; domínio não depende de HTTP, ORM, Kafka ou Redis; check de URL remota síncrono do ponto de vista do cliente MAS implementado com client HTTP não-bloqueante (reativo) — NÃO reintroduzir anti-padrão de I/O bloqueante diagnosticado em `arquitetura-solucao.md` item 4; exclusão de MediaItem vinculado a Event SEMPRE permitida com desvinculação automática (nunca retornar 409 neste caso).

**Scale/Scope**: CRUD de `MediaItem` (criar com validação/fallback, consultar individual, listar paginado, excluir), catálogo extensível de `media_type_catalog` consumido via FK/validação de domínio, cache-aside Redis individual com invalidação síncrona, contrato de erro RFC 7807, validação de role `ROLE_ADMIN` em escritas, hooks de observabilidade para latência/erros do check remoto.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- Layer boundaries: PASS. A implementação deve separar `domain` (entidades MediaItem/MediaTypeCatalog, especificações de validação de URL, eventos de domínio sem framework), `application` (casos de uso Create/Get/List/DeleteMediaItem, portas de saída para MediaItemRepository/RemoteUrlValidator/MediaTypeCatalogReader/MediaItemCache), `adapter-in/rest` (recursos REST com anotações Quarkus/Jackson, mapeamento de exceptions para RFC 7807) e `adapter-out` (persistence Panache, cache Redis, validador de URL remoto via WebClient reativo). Entidades de domínio e validadores não devem depender de Quarkus, JPA/Panache, annotations HTTP ou Redis.
- Contract-first scope: PASS. Contrato REST admin v1 definido em `contracts/openapi.yaml` (POST/DELETE/GET /api/v1/admin/media-items e GET /api/v1/media-items para leitura pública) incluindo payloads de requisição/resposta, códigos de status (201, 204, 400, 401/403, 404, 409) e formato RFC 7807 para erros. Cache Redis usa chave `catalog:media-item:{id}` documentada em FR-007. Sem novos eventos Kafka nesta feature (nenhum contrato de evento pendente de aprovação).
- Test depth: PASS. Unitários de domínio/validação de URL e fallback, contrato REST OpenAPI, integração com Testcontainers PostgreSQL+Redis para constraints `uq_media_item_url`/`ck_media_item_url_scheme` e fluxo de exclusão cascata SET NULL, e E2E P1 com mock de servidor remoto HTTP cobrindo sucesso e falha/timeout do check.
- UX consistency: PASS. RFC 7807 em todas as falhas de validação/negócio; 401/403 para auth; 404 para item inexistente; 409 para URL duplicada; paginação base 0 em listagem; UUIDs v4 em identificadores; payload de resposta sempre retorna `url`, `fallbackApplied` e `cachedFileName` para transparência de fallback.
- Performance budgets: PASS. Orçamentos (p95 escrita <=300ms, timeout remoto <=250ms não bloqueante, erro 5xx <0,1%) e validação por métricas Prometheus + testes de integração + smoke local estão definidos. Timeout 250ms é orçamento específico do check remoto documentado no Edge Cases.

Post-design re-check: PASS. Artefatos de fase 1 confirmam aderência: (a) data-model.md separa MediaItem aggregate root (domínio) de MediaTypeCatalog (tabela de domínio/seed), sem dependência de framework nas entidades; (b) contracts/openapi.yaml define contrato v1 completo com rotas admin/públicas separadas, security bearerAuth, Problem RFC 7807 em todas respostas de erro, e `MediaItemCreate` sem campos read-only (fallbackApplied, cachedFileName); (c) quickstart.md mapeia cobertura de testes unitários/contrato/integração Testcontainers Postgres+Redis/E2E P1 com WireMock para os dois cenários de sucesso e falha/timeout do check remoto; (d) UX consistency garantida (RFC 7807, 401/403, 404, 409, paginação base 0, UUIDs, fallback transparente na response); (e) orçamentos de performance documentados (250ms URL check, p95 ≤300ms escrita) com método de validação por métricas Prometheus + testes. Nenhuma violação ou exceção a expirar.

## Project Structure

### Documentation (this feature)

```text
microservice-catalog/specs/012-cadastrar-itens-midia/
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
│   ├── catalog/media/domain/
│   │   ├── MediaItem.java
│   │   ├── MediaTypeCatalog.java
│   │   ├── MediaItemRepositoryPort.java
│   │   ├── MediaTypeCatalogReaderPort.java
│   │   ├── MediaItemCachePort.java
│   │   └── RemoteUrlValidatorPort.java
│   ├── catalog/media/application/
│   │   ├── CreateMediaItemUseCase.java
│   │   ├── GetMediaItemUseCase.java
│   │   ├── ListMediaItemsUseCase.java
│   │   └── DeleteMediaItemUseCase.java
│   ├── catalog/media/adapter/in/rest/
│   │   ├── MediaItemAdminResource.java
│   │   ├── MediaItemPublicResource.java
│   │   ├── dto/
│   │   │   ├── CreateMediaItemRequest.java
│   │   │   └── MediaItemResponse.java
│   │   └── exception/
│   │       └── MediaItemExceptionMapper.java
│   └── catalog/media/adapter/out/
│       ├── persistence/
│       │   ├── PanacheMediaItemRepository.java
│       │   ├── PanacheMediaTypeCatalogReader.java
│       │   └── entity/
│       │       ├── MediaItemEntity.java
│       │       └── MediaTypeCatalogEntity.java
│       ├── cache/
│       │   └── RedisMediaItemCache.java
│       └── urlcheck/
│           └── VertxRemoteUrlValidator.java
├── src/main/resources/
│   ├── application.properties
│   │   └── (properties: catalog.media.url-check.timeout-ms=250,
│   │       catalog.media.fallback-file=not_available.jpg,
│   │       catalog.media.cache.ttl-seconds=3600)
│   └── db/
│       ├── changelog/db.changelog-master.xml
│       └── migration/V1__init.sql (já contém DDL de media_item/media_type_catalog)
└── src/test/java/br/vsjr/labs/ticketmoster/
    ├── catalog/media/domain/
    ├── catalog/media/application/
    ├── catalog/media/adapter/in/rest/
    └── catalog/media/adapter/out/
        ├── persistence/ (IT com Testcontainers Postgres)
        ├── cache/ (IT com Testcontainers Redis)
        └── urlcheck/ (unit com mock WebClient)
```

**Structure Decision**: Usar módulo único Quarkus do `microservice-catalog` com pacotes por agregado `catalog.media.*` e fronteiras lógicas de clean architecture (domain/application/adapter-in/adapter-out). O `V1__init.sql` já contém a DDL correta (`media_type_catalog`, `media_item`, FK em `event.media_item_id ... ON DELETE SET NULL`); nenhuma migração adicional é necessária nesta feature. A verificação de URL remota usa `Vertx WebClient` reativo já incluído transitivamente pela stack Quarkus REST/HTTP, evitando dependência de `quarkus-rest-client-reactive` para manter footprint pequeno.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Dupla validação de esquema de URL (CHECK constraint no Postgres + validador de domínio prévio) | Constraint banco é lastro para dados bypassando API; validador de domínio retorna erro 400 amigável RFC 7807 ao invés de erro genérico de constraint | Apenas CHECK banco retornaria ConstraintViolationException mapeada para 500/erro genérico sem título RFC 7807 específico de esquema inválido |
| Cache-aside individual mesmo para recurso admin-centric | FR-007 exige explicitamente cache Redis sob `catalog:media-item:{id}` invalidado sincronamente | Sem cache, consultas repetidas de item de mídia (ex.: detalhe de evento com mídia) gerariam carga desnecessária no Postgres |
| Fallback aplicado no domínio (cachedFileName setado em caso de falha remota) não só na camada de apresentação | Especificação exige persistir o fallback no MediaItem (`fallback_applied = true` e `cachedFileName` associado) para que consumo público também veja o fallback sem repetir o check remoto | Fallback só na exibição exigiria check remoto a cada renderização de evento na API pública, expondo clientes a latência e novos timeouts |
