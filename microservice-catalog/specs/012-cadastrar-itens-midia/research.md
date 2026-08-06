# Research: US-CAT-12 - Cadastrar Itens de Mídia com Validação e Fallback

## Decision: Usar Vert.x WebClient reativo (não quarkus-rest-client-reactive) para validação remota de URL

**Rationale**: A stack Quarkus REST já traz `io.vertx.vertx-web-client` transitivamente via `quarkus-rest` / `quarkus-vertx-http`. O `WebClient` nativo do Vert.x é menos intrusivo, não requer `@RegisterRestClient`, e expõe API reativa nativa (`Single<HttpResponse<Buffer>>` convertido para `Uni<Boolean>`) com timeout configurável por chamada. O SmallRye Fault Tolerance (`@Timeout`) pode ser aplicado opcionalmente no adapter, mas o timeout nativo do WebClient (`.timeout(250)`) já cobre o requisito sem anotação adicional.

**Alternatives considered**: `quarkus-rest-client-reactive` + `@Timeout` do SmallRye FT foi considerado porque já existe a extensão no pom.xml (`quarkus-smallrye-fault-tolerance`), mas essa abordagem exigiria criar uma interface `@RestClient` com endpoint "placeholder" genérico (não há um host fixo para validar — as URLs são arbitrárias). A interface `@RestClient` é tipada por host/base URI, inadequada para validação de URL dinâmica. A opção `HttpClient` síncrono do Java 11+ foi rejeitada por bloquear a thread event loop, exatamente o anti-padrão diagnosticado em `arquitetura-solucao.md` item 4.

## Decision: Verificação síncrona do ponto de vista do cliente, implementada de forma não-bloqueante (reativa) a nível de thread

**Rationale**: A spec declara explicitamente que a verificação é "síncrona do ponto de vista do fluxo de resposta ao cliente" (o admin aguarda o resultado antes de receber 201), mas MÚST ser implementada "com client HTTP não bloqueante" para não reintroduzir o anti-padrão do legado `MediaManager` (Diagnóstico item 4). O fluxo implementado: `CreateMediaItemUseCase` retorna `Uni<MediaItem>`; dentro do pipeline reativo, o `RemoteUrlValidatorPort.validate(url)` retorna `Uni<Boolean>` usando WebClient sem bloquear. O cliente recebe 201 depois que o check conclui (ou falha/timeout), mas a thread do event loop é liberada durante a I/O.

**Alternatives considered**: Check assíncrono com retorno 202 Accepted + notificação foi rejeitado por contrariar US1/AC1 e US1/AC2 que esperam 201 Created com dados completos. Blocking HTTP client foi rejeitado por violar "Arquitetura Reativa" seção 12 e o diagnóstico do legado.

## Decision: Dupla validação de URL (validador de domínio prévio + CHECK constraint PostgreSQL)

**Rationale**: O CHECK constraint `ck_media_item_url_scheme CHECK (url ~* '^https?://')` já existe no `V1__init.sql` como lastro estrutural para qualquer bypass de API. No entanto, confiar só na constraint resulta em `ConstraintViolationException` mapeada para erro genérico (potencialmente 500). O validador de domínio (`UrlValidator.isValidScheme(url)`) verifica o esquema antes da persistência e lança `InvalidMediaUrlException` que o `ExceptionMapper` converte para RFC 7807 com `title="Esquema de URL inválido"`, `status=400` e detalhamento específico. O mesmo padrão vale para unicidade: aplicação verifica prévio para 409 amigável, banco mantém `uq_media_item_url` para concorrência simultânea.

**Alternatives considered**: Apenas validação em domínio foi rejeitada por deixar dados que bypassam a API sem proteção estrutural. Apenas constraint banco foi rejeitada por produzir mensagens de erro pouco acionáveis para o admin.

## Decision: Fallback aplicado e persistido no MediaItem (não só na camada de leitura/apresentação)

**Rationale**: A spec (RN35, CA-CAT-03-MED) exige que a URL original seja persistida, `fallback_applied` marcado, e o fallback `not_available.jpg` associado quando a resolução remota falha. Persistir esse estado significa que (a) o consumo público de detalhe do evento não precisa repetir o check remoto a cada leitura, (b) métricas de `fallback_applied` podem ser consultadas via SQL/Provenance sem reexecutar I/O, e (c) o comportamento é determinístico: uma vez marcado como fallback, permanece até o admin editar manualmente o `MediaItem`.

**Alternatives considered**: Fallback aplicado dinamicamente na leitura pública foi rejeitado porque exigiria HTTP check a cada renderização de evento (impacto latência público, risco de timeout repetido, falta de transparência por que o admin não sabe se uma mídia está falhando). Cache transitório de resultado de check foi rejeitado por perder o estado entre reinícios e não ser audível.

## Decision: Cache-aside Redis com chave `catalog:media-item:{id}` (FR-007), invalidação síncrona em alteração/exclusão

**Rationale**: FR-007 define explicitamente essa chave como nova (não herdada de `arquitetura-solucao.md` seção 10). A estratégia cache-aside: GetMediaItemUseCase primeiro consulta Redis, se miss consulta Postgres e grava cache; Create/Delete invalidam a chave imediatamente após transação bem-sucedida (validação síncrona de invalidação, não eventual). Observação: listagens paginadas não são cacheadas nesta feature (consistência paginada cacheada pode ser adicionada depois com contrato próprio).

**Alternatives considered**: Write-through em create foi considerado, mas invalidação simples é mais resiliente (criação admin é rara, leitura de detalhe por evento é frequente). TTL longo (1h) sem invalidação ativa foi rejeitado por risco de dado inconsistente após exclusão/edição.

## Decision: Exclusão de MediaItem vinculado a Event SEMPRE permitida com desvinculação SET NULL (nunca 409)

**Rationale**: A DDL de origem define `event.media_item_id UUID NULL REFERENCES catalog.media_item(id) ON DELETE SET NULL` e RN05 diz que mídia é opcional. A especificação corrige a versão anterior que descrevia erroneamente comportamento conflitante. A implementação deve: (a) deletar o MediaItem via Panache (o próprio Postgres cuida do SET NULL na FK automaticamente, sem query adicional no código), (b) invalidar cache do MediaItem, e (c) invalidar cache de Event correspondente (se existir contrato para chave `catalog:event:{id}`). Se não houver cache de Event, só invalidar cache de MediaItem e marcar dependência para tarefa futura.

**Alternatives considered**: Bloquear exclusão com 409 quando houver vinculo foi rejeitado por contrariar a DDL ON DELETE SET NULL e a correção documentada na spec. Consultar préviamente a tabela event para emitir aviso foi considerado opcional (log info), não bloqueante.

## Decision: media_type_catalog é tabela de domínio seedada/migração; sem API self-service de escrita nesta feature

**Rationale**: A spec (ver Assumptions item 4 e a nota de escopo da US2) declara que inclusão de novos tipos (ex.: `VIDEO`) é operação de migração de banco/deploy, não API. A aplicação apenas CONSOME a tabela: `MediaTypeCatalogReaderPort.findByCodeAndEnabled(code)` retorna `Optional<MediaTypeCatalog>`; se vazio ou desabilitado, `CreateMediaItemUseCase` lança `InvalidMediaTypeException` → 400 RFC 7807. O `V1__init.sql` já contém o seed `INSERT INTO catalog.media_type_catalog VALUES ('IMAGE', 'Imagem promocional', TRUE)`.

**Alternatives considered**: CRUD completo de media_type_catalog exposto ao admin foi rejeitado pela spec; inclusão de `VIDEO`/`AUDIO` pertence a tarefa US-CAT-14 separada ou migração Liquibase. Enum Java fechado `MediaType` foi rejeitado por não atender `[ALTERA RN34]` (extensível sem redeploy).

## Decision: `cachedFileName` como identificador lógico (chave de objeto), não caminho de sistema de arquivos

**Rationale**: `arquitetura-solucao.md` seção 16 declara que o `tmpDir` do legado é substituído por object storage (S3/MinIO), mas nenhuma spec define contrato de bucket/chave. Esta feature define `cachedFileName` como VARCHAR(255) na DDL — quando `fallback_applied = true`, valor padrão é `not_available.jpg` (asset estático servido pelo serviço ou chave de bucket). A resolução de URL pública do cachedFileName (ex.: prefixo `https://cdn.ticketmonster.io/assets/`) é configuração fora do escopo desta feature e pode ser aplicada no `MediaItemResponse` adapter via propriedade `catalog.media.cache.url-prefix`.

**Alternatives considered**: Armazenar caminho absoluto de disco foi rejeitado por violar 12-Factor (sem estado local) e a decisão da seção 16. Resolver backend de armazenamento agora (S3 client integration) foi rejeitado por ser decisão de infraestrutura fora do escopo funcional.

## Decision: E2E P1 usa WireMock (mock de servidor HTTP remoto) para sucesso e falha/timeout

**Rationale**: Quarkus oferece `quarkus-test-wiremock` (ou WireMock standalone via `@TestResource`) como forma estável de simular dois cenários do US1: (a) rota `/ok.jpg` retornando 200 OK com cabeçalhos válidos → `fallback_applied = false`; (b) rota `/slow.jpg` com `withFixedDelay(2000)` para estourar timeout 250ms → `fallback_applied = true`, e rota `/notfound.jpg` retornando 404 → também `fallback_applied = true`. Isso evita dependência de rede em CI e torna o teste determinístico.

**Alternatives considered**: Usar `www.google.com` ou URL pública qualquer foi rejeitado por gerar falso negativo em CI offline. Criar mock `RemoteUrlValidatorPort` em código sem mockar servidor HTTP foi rejeitado por não testar a integração real do Vert.x WebClient adapter (serialização de timeout, mapeamento de falha de rede).
