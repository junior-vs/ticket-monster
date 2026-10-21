# Quickstart: US-CAT-12 - Cadastrar Itens de Mídia com Validação e Fallback

## Pré-requisitos

- Docker em execução (para Testcontainers e/ou docker-compose).
- Java runtime compatível com `microservice-catalog/pom.xml` (Java 25 release).
- Maven wrapper ou Maven local disponível no módulo.
- JWT admin com role `ROLE_ADMIN` para testes manuais das rotas POST/DELETE.
- (Opcional) Token público sem role para testar rotas GET /media-items.

## Setup Local

A partir da raiz do repositório:

```powershell
docker compose -f docker-compose.shared.yml up -d
```

A partir de `microservice-catalog`:

```powershell
.\mvnw.cmd test
.\mvnw.cmd quarkus:dev
```

O serviço utiliza:

- PostgreSQL `catalog_db` (schema `catalog`; `V1__init.sql` já contém tabelas `media_type_catalog` e `media_item` com constraints).
- Redis DB 0 (cache-aside individual: `catalog:media-item:{id}`).
- Kafka topic `catalog-events` (não usado nesta feature; preparado para propagandas futuras).
- Health endpoints: `/q/health/live`, `/q/health/ready`.
- Métricas: `/q/metrics`.

**Properties relevantes em `application.properties`**:

| Chave | Valor padrão | Descrição |
|---|---|---|
| `catalog.media.url-check.timeout-ms` | `250` | Timeout em ms para o check remoto da URL durante criação. |
| `catalog.media.fallback-file` | `not_available.jpg` | Chave lógica / nome do asset usado quando `fallbackApplied = true`. |
| `catalog.media.cache.ttl-seconds` | `3600` | TTL do cache Redis individual. |

## Validação de Contrato

O contrato REST está em `microservice-catalog/specs/012-cadastrar-itens-midia/contracts/openapi.yaml`.

Checagens esperadas:

- `MediaItemCreate` contém apenas `mediaTypeCode` e `url` (não aceita `fallbackApplied` nem `cachedFileName` na entrada).
- `MediaItem.fallbackApplied` e `MediaItem.cachedFileName` são retornados apenas em responses (read-only).
- Rotas `POST /admin/media-items` e `DELETE /admin/media-items/{id}` exigem Bearer token (segurança aplicada; `@RolesAllowed("ROLE_ADMIN")`).
- Rotas `GET /media-items` e `GET /media-items/{id}` são públicas (sem `security`).
- Erros usam `application/problem+json` (RFC 7807).
- DELETE nunca retorna 409 (ver cenário 4).

---

## Cenário 1: Criar MediaItem com URL Acessível (P1 / US1 AC1)

```powershell
$token = '<admin-jwt-ROLE_ADMIN>'
Invoke-RestMethod `
  -Method Post `
  -Uri 'http://localhost:8080/api/v1/admin/media-items' `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType 'application/json' `
  -Body '{"mediaTypeCode":"IMAGE","url":"https://placehold.co/600x400.jpg"}'
```

**Esperado**:

- HTTP 201 Created.
- Response contém UUID `id`.
- `fallbackApplied: false`.
- `cachedFileName: null` (ou opcionalmente vazio, não `not_available.jpg`).
- Requisitando a mesma URL imediatamente em outro POST → HTTP 409 Conflict com `title="URL já cadastrada"`.

---

## Cenário 2: Criar MediaItem com URL Inacessível e Verificar Fallback Automático (P1 / US1 AC2 + CA-CAT-03-MED)

Dois sub-cenários cobrem o mesmo requisito (ambos devem aplicar fallback e retornar 201):

### 2a — URL remota retorna 404

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri 'http://localhost:8080/api/v1/admin/media-items' `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType 'application/json' `
  -Body '{"mediaTypeCode":"IMAGE","url":"https://httpstat.us/404"}'
```

### 2b — URL remota com delay > 250 ms (timeout)

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri 'http://localhost:8080/api/v1/admin/media-items' `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType 'application/json' `
  -Body '{"mediaTypeCode":"IMAGE","url":"https://httpstat.us/200?sleep=2000"}'
```

**Esperado para ambos**:

- HTTP 201 Created (NÃO 500, NÃO 400 — o fallback suprime a falha remota).
- Response contém `fallbackApplied: true`.
- `cachedFileName: "not_available.jpg"` (asset de fallback padrão).
- `url` persistida é a URL original (mesmo inacessível; transparência para o admin).
- Endpoint não bloqueia a thread (verificado via teste concorrente ou log de event-loop não sobrecarregado).

---

## Cenário 3: Rejeições em Criação (RN37 / FR-005)

### 3a — Esquema inválido (ftp://)

```powershell
try {
  Invoke-RestMethod `
    -Method Post `
    -Uri 'http://localhost:8080/api/v1/admin/media-items' `
    -Headers @{ Authorization = "Bearer $token" } `
    -ContentType 'application/json' `
    -Body '{"mediaTypeCode":"IMAGE","url":"ftp://files.local/banner.png"}'
} catch {
  $_.Exception.Response.StatusCode.value__  # Esperado: 400
  $_.ErrorDetails.Message | ConvertFrom-Json  # Esperado: title="Esquema de URL inválido"
}
```

### 3b — Tipo de mídia inexistente ou desabilitado

```powershell
# 'DOCX' não está no seed e não está habilitado
Invoke-RestMethod `
  -Method Post `
  -Uri 'http://localhost:8080/api/v1/admin/media-items' `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType 'application/json' `
  -Body '{"mediaTypeCode":"DOCX","url":"https://example.com/file.docx"}'
```

**Esperado**: HTTP 400 `title="Tipo de mídia inválido ou inativo"`.

### 3c — Sem token admin

```powershell
try {
  Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/v1/admin/media-items' -ContentType 'application/json' -Body '{"mediaTypeCode":"IMAGE","url":"https://example.com/x.jpg"}'
} catch {
  $_.Exception.Response.StatusCode.value__  # Esperado: 401
}
```

---

## Cenário 4: Excluir MediaItem Vinculado a Evento (US3 / FR-008)

Este cenário depende da criação de um Event com media_item_id. A exclusão SEMPRE deve funcionar com desvinculação automática (nunca 409):

```powershell
$mediaId = '<media-item-id-usado-em-um-Event>'
Invoke-WebRequest `
  -Method Delete `
  -Uri "http://localhost:8080/api/v1/admin/media-items/$mediaId" `
  -Headers @{ Authorization = "Bearer $token" }
```

**Esperado**:

- HTTP 204 No Content.
- Consulta direta no Postgres: `SELECT media_item_id FROM catalog.event WHERE media_item_id = '$mediaId'` → zero rows (todos foram `SET NULL`).
- Chave Redis `catalog:media-item:{id}` foi deletada (DEL síncrono após sucesso da transação).
- Consultar `GET /media-items/{id}` agora retorna 404.

---

## Cenário 5: Cache Redis e Leitura Pública (FR-007)

```powershell
# 1. Consulta pública (primeiro acesso) → cache miss → banco → grava cache
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/v1/media-items/$mediaId"

# 2. Validar no redis-cli: GET catalog:media-item:{id} → deve existir

# 3. Excluir item
Invoke-WebRequest -Method Delete -Uri "http://localhost:8080/api/v1/admin/media-items/$mediaId" `
  -Headers @{ Authorization = "Bearer $token" }

# 4. Validar no redis-cli: GET catalog:media-item:{id} → (nil) (invalidação síncrona)
```

---

## Testes Automatizados Esperados

Executar de `microservice-catalog`:

```powershell
.\mvnw.cmd test
```

Cobertura obrigatória por FR-006:

- **Unitários de domínio**:
  - `UrlValidator.isValidScheme(...)` → verdadeiro para `http://` e `https://`; falso para `ftp://`, sem esquema, vazio.
  - Regra de fallback: quando `RemoteUrlValidatorPort.validate(...)` retorna falha/timeout → `fallbackApplied = true` e `cachedFileName = "not_available.jpg"` no construtor/fábrica de `MediaItem`.
  - Validação de tipo: `mediaTypeCode` ausente em `media_type_catalog` lança exceção de domínio.
- **Contrato REST**:
  - 201 (sucesso/timeout/falha remota), 204 (delete), 400 (esquema/tipo), 401/403 (rota admin), 404 (não encontrado), 409 (URL duplicada).
  - Schema `MediaItemCreate` não contém `fallbackApplied` nem `cachedFileName`.
- **Integração com Testcontainers Postgres**:
  - Constraint `uq_media_item_url` dispara 409 em concorrência.
  - Constraint `ck_media_item_url_scheme` rejeita URL não http(s) que bypassou a validação de domínio (cenário de bypass de API).
  - DELETE de MediaItem vinculado a Event → evento fica com `media_item_id = NULL` (SELECT de validação).
- **Integração com Testcontainers Redis**:
  - Cache miss → hit no banco → cache preenchido.
  - DELETE sincroniza com DEL da chave.
- **E2E P1 (obrigatório por Constituição III)**:
  - Usando WireMock para servidor HTTP mockado:
    - Cenário A: rota `/ok.jpg` retorna 200 → `fallbackApplied = false`.
    - Cenário B: rota `/slow.jpg` com delay 2000ms (timeout) → `fallbackApplied = true`, `cachedFileName = "not_available.jpg"`, response 201.
    - Cenário C: rota `/notfound.jpg` retorna 404 → `fallbackApplied = true`, response 201.

## Risco de Release Conhecido

- Backend de object storage (S3/MinIO) para resolver `cachedFileName` em URL pública (ex.: prefixo CDN) é decisão de infraestrutura fora do escopo desta feature; o campo é persistido como chave lógica e a resolução de URL final pode ser adicionada no adapter de response em tarefa posterior.
- `media_type_catalog` não tem API de escrita nesta feature; novos tipos exigem migração Liquibase/deploy.
- Nenhum evento Kafka é publicado nesta feature (`MediaItemCreatedEvent`, `MediaItemDeletedEvent`); se necessário para sincronia cross-service, exigir contrato e ADR próprios.
