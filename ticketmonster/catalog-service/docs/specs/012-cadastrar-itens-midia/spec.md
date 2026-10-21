# Feature Specification: Cadastrar Itens de Mídia com Validação e Fallback (Admin)

**Feature Branch**: `012-cadastrar-itens-midia`
**Created**: 2026-07-25
**Status**: Draft
**Input**: User description: "* **US-CAT-12:** Cadastrar novos itens de mídia com validação síncrona de URL (Admin). respect: docs\\spec\\microservice-catalog.spec.md, docs\\arch\\arquitetura-solucao.md"

**Origem**: `microservice-catalog_spec.md` — US-CAT-12, RN34, RN35, RN37, CA-CAT-03-MED; `[ALTERA RN34]` / US-CAT-14 (catálogo extensível de tipos).

## Extensões e correções declaradas nesta spec

1. **Exclusão de `MediaItem` vinculado a evento — corrigida**: a DDL de origem define `event.media_item_id ... ON DELETE SET NULL` (RN05 — mídia é opcional). Isso significa que a exclusão de um `MediaItem` **é permitida mesmo estando vinculado**; o evento apenas perde a referência de mídia. A versão anterior desta spec descrevia erroneamente `ON DELETE SET NULL` e `ON DELETE RESTRICT` como alternativas equivalentes para "impedir a exclusão" — são comportamentos opostos. Esta versão segue a decisão já tomada na DDL (`SET NULL`) e corrige o cenário de aceite (ver US3/AC2).
2. **"Verificação síncrona" da URL não significa bloqueio de thread**: `arquitetura-solucao.md` (Diagnóstico Arquitetural, item 4) identifica como problema do legado exatamente threads de I/O bloqueadas por chamadas HTTP externas síncronas no `MediaManager`. Esta spec declara explicitamente que a verificação, embora síncrona do ponto de vista do fluxo de resposta ao cliente (o admin aguarda o resultado antes de receber 201), MUST ser implementada de forma não bloqueante ao nível de thread (client HTTP reativo / `Uni`, consistente com a seção 12 — Arquitetura Reativa), para não reintroduzir o anti-padrão diagnosticado.
3. **Armazenamento físico de mídia não resolvido nos documentos-fonte**: `arquitetura-solucao.md` seção 16 declara que o cache em arquivo local (`tmpDir`) do legado é substituído por object storage (S3/MinIO), mas nenhuma spec de microsserviço define contrato de bucket/chave. Esta spec assume `cachedFileName` como um identificador lógico (chave de objeto), não um caminho de disco, e trata a resolução completa do backend de armazenamento como fora de escopo desta feature (ver Assumptions).
4. **Escopo de `media_type_catalog` explicitado**: é seed de dados administrado por migração/deploy, não uma API self-service desta feature (ver Assumptions).
5. **Métricas de latência movidas para Assumptions** (sem lastro em `arquitetura-solucao.md`).
6. **Contrato de chave de cache Redis explicitado** (FR-007), ausente na seção 10 do documento de arquitetura.
7. **Tabela de rastreabilidade** adicionada ao final.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cadastrar Novo Item de Mídia com Validação e Fallback (Priority: P1)

Como um Administrador do sistema, quero cadastrar um novo item de mídia promocional informando a URL e o tipo de mídia, para que o sistema valide a estrutura da URL e aplique automaticamente uma imagem de fallback em caso de inacessibilidade remota, garantindo que o catálogo nunca exiba links quebrados.

**Why this priority**: Imagens e banners promocionais são indispensáveis para a atratividade visual do catálogo de vendas de eventos. O mecanismo de fallback garante resiliência visual mesmo quando servidores externos de imagem falham.

**Independent Test**: Pode ser testado de forma independente efetuando requisições HTTP POST para criar um `MediaItem` com uma URL válida. Testar dois cenários: 1) URL remota acessível (`fallback_applied = false`), 2) URL remota inacessível (`fallback_applied = true` e indicação do arquivo local de fallback `not_available.jpg`).

**Acceptance Scenarios**:

1. **Given** um Administrador autenticado com a role `ROLE_ADMIN`, **When** ele envia uma solicitação de criação de item de mídia com uma URL no formato `http://` ou `https://` válida e acessível (RN37) e tipo `IMAGE`, **Then** o sistema deve salvar o item com `fallback_applied = false` e retornar HTTP 201 (Created) com os dados cadastrados.
2. **Given** um Administrador autenticado, **When** ele cadastra uma URL estruturalmente válida, porém cujo servidor remoto não responde (timeout ou erro HTTP 404/500) (CA-CAT-03-MED), **Then** o sistema NÃO DEVE travar o cadastro nem bloquear a thread de atendimento; DEVE persistir a URL original, marcar `fallback_applied = true`, associar o fallback local (`not_available.jpg`) (RN35) e retornar HTTP 201 (Created).
3. **Given** um Administrador autenticado, **When** ele tenta cadastrar um item de mídia com uma URL que já existe na base de dados (RN37), **Then** o sistema deve rejeitar o cadastro e retornar erro HTTP 409 (Conflict) formatado via RFC 7807 (Problem Details).
4. **Given** um Administrador autenticado, **When** ele envia um esquema de URL inválido (ex.: `ftp://` ou texto malformatado) (RN37), **Then** o sistema deve rejeitar a requisição e retornar HTTP 400 (Bad Request).

---

### User Story 2 - Suportar Catálogo Extensível de Tipos de Mídia (Vídeo/Áudio) (Priority: P2)

Como um Administrador do sistema, quero cadastrar itens de mídia promocional de diferentes tipos (como vídeo ou áudio), utilizando um catálogo de tipos extensível, para enriquecer o acervo promocional dos eventos sem exigir alterações de código no microsserviço.

**Why this priority**: Atende à evolução `[ALTERA RN34]` e US-CAT-14, permitindo que mídias em vídeo ou áudio sejam vinculadas a atrações artísticas.

**Independent Test**: Pode ser testado cadastrando um `MediaItem` com `media_type_code = 'VIDEO'` habilitado no banco e confirmando a persistência com o tipo correto.

> Nota de escopo: esta US cobre apenas o *consumo* de `media_type_code` já existente no catálogo de tipos. A *inclusão* de um novo tipo (ex.: `VIDEO`) é operação de migração/seed de banco, fora do escopo de API desta feature — ver Assumptions.

**Acceptance Scenarios**:

1. **Given** o tipo de mídia `VIDEO` ativo no catálogo de tipos (`media_type_catalog`), **When** o Administrador cadastra um item de mídia especificando `media_type_code = 'VIDEO'`, **Then** o sistema deve validar a existência do tipo no catálogo de domínio e cadastrar o recurso com HTTP 201 (Created).
2. **Given** uma solicitação de cadastro, **When** o Administrador informa um código de tipo de mídia inexistente ou desabilitado (ex.: `DOCX`), **Then** o sistema deve retornar HTTP 400 (Bad Request).

---

### User Story 3 - Consultar e Remover Itens de Mídia (Priority: P3)

Como um Administrador do sistema, quero listar itens de mídia cadastrados e remover itens obsoletos que não estão mais em uso, para manter o acervo de mídias organizado.

**Why this priority**: Permite o gerenciamento e reutilização de itens de mídia cadastrados no painel administrativo.

**Independent Test**: Pode ser testado listando itens de mídia via HTTP GET e excluindo um item, vinculado ou não a um evento, via HTTP DELETE.

**Acceptance Scenarios**:

1. **Given** um item de mídia cadastrado que não possui vínculo com nenhum evento, **When** o Administrador envia uma solicitação HTTP DELETE, **Then** o sistema deve remover o item de mídia e retornar HTTP 204 (No Content).
2. **Given** um item de mídia vinculado a um ou mais eventos, **When** o Administrador envia uma solicitação HTTP DELETE, **Then** o sistema DEVE permitir a exclusão (consistente com `event.media_item_id ON DELETE SET NULL` — RN05, mídia opcional), remover o `MediaItem`, desvincular automaticamente os eventos associados (`media_item_id` passa a `NULL`) e retornar HTTP 204 (No Content). O sistema NÃO deve retornar 409 neste caso.

---

### Edge Cases

- **Timeout no check síncrono da URL remota**: o tempo limite para a resolução HTTP da URL remota durante o cadastro não deve ultrapassar 250 ms, executado via client HTTP não bloqueante (ver "Extensões e correções", item 2). Se estourar o timeout, a requisição é tratada como falha de resolução remota e a mídia é gravada com `fallback_applied = true` sem falhar o endpoint do admin (CA-CAT-03-MED).
- **Tentativa de cadastro de URL com esquema proibido**: URLs que não iniciem por `http://` ou `https://` devem ser rejeitadas via constraint no banco de dados (`ck_media_item_url_scheme`) e validação prévia de domínio (RN37).
- **Concorrência ao tentar cadastrar a mesma URL simultaneamente**: o banco de dados garante a unicidade via `uq_media_item_url` retornando HTTP 409 Conflict.
- **Exclusão de mídia vinculada a evento publicado**: a desvinculação (`SET NULL`) faz o evento passar a exibir o fallback local na leitura pública (RN35) até que um novo `MediaItem` seja associado — não há bloqueio de exclusão neste fluxo.
- **Acesso sem a role `ROLE_ADMIN`**: deve retornar HTTP 401 (Unauthorized) ou 403 (Forbidden).

### User Experience Consistency *(mandatory)*

- **Canais**: as operações de escrita (POST, DELETE) de itens de mídia são restritas a `ROLE_ADMIN`. A leitura e consumo das URLs tratadas/fallback são expostas ao catálogo público.
- **Transparência de Fallback**: o cliente da API recebe no objeto retornado da mídia os campos `url` (URL original persistida), `fallbackApplied` (booleano) e `cachedFileName` (identificador lógico do recurso de fallback/cache — não necessariamente caminho de disco; ver "Extensões e correções", item 3).
- **Formato de Erros**: qualquer violação de validação é exposta via RFC 7807 (Problem Details).
- **Identificadores**: itens de mídia utilizam UUID v4.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema DEVE fornecer endpoints REST administrativos restritos a `ROLE_ADMIN` para cadastro, consulta e exclusão de itens de mídia (`MediaItem`).
- **FR-002**: O sistema DEVE exigir obrigatoriamente que a URL de um item de mídia possua esquema `http://` ou `https://` e seja única em toda a base de dados (RN37), retornando HTTP 409 (Conflict) para URLs duplicadas e HTTP 400 (Bad Request) para esquemas inválidos.
- **FR-003**: O sistema DEVE realizar verificação de acessibilidade da URL remota durante o cadastro, de forma síncrona do ponto de vista do fluxo de resposta ao cliente, porém implementada com client HTTP não bloqueante (reativo). Caso a resolução remota falhe ou estoure o tempo limite de 250 ms, o sistema DEVE persistir a URL original, definir `fallback_applied = true` e vincular a imagem de fallback local (`not_available.jpg`) sem interromper ou travar o cadastro (RN35, CA-CAT-03-MED).
- **FR-004**: O sistema DEVE suportar um catálogo extensível de tipos de mídia (`IMAGE`, `VIDEO`, `AUDIO`, etc.) gerenciado pela tabela de domínio `media_type_catalog`, validando os tipos de mídia aceitos sem necessidade de alteração de código ou redeploy do serviço (`[ALTERA RN34]`, US-CAT-14). A inclusão de novos códigos de tipo é operação de migração de dados, fora do escopo de API desta feature.
- **FR-005**: O sistema DEVE responder com o formato estrito RFC 7807 (Problem Details) para todas as ocorrências de erros de validação ou conflitos de URL.
- **FR-006**: O sistema DEVE incluir suíte de testes automatizados completa: testes unitários da lógica de validação de URL e fallback, testes de contrato REST, testes de integração com Testcontainers PostgreSQL e Redis, e um teste E2E cobrindo a jornada P1 com mock de servidor remoto (sucesso e falha de download).
- **FR-007**: O sistema DEVE manter cache-aside em Redis para leitura de itens de mídia individuais sob a chave `catalog:media-item:{id}`, invalidada de forma síncrona sempre que o item for alterado ou removido. Contrato de chave novo, não definido em `arquitetura-solucao.md` seção 10.
- **FR-008**: A exclusão de um `MediaItem` vinculado a um ou mais `Event` MUST ser permitida e MUST desvincular automaticamente a referência (`event.media_item_id = NULL`) nos eventos afetados, consistente com `ON DELETE SET NULL` da DDL de origem. O sistema MUST NOT retornar 409 neste caso.

### Key Entities *(include if feature involves data)*

- **MediaTypeCatalog (Tabela de Domínio Extensível — seed de dados, sem API de escrita nesta feature)**:
  - `code`: VARCHAR(30) (Chave primária, ex.: 'IMAGE', 'VIDEO', 'AUDIO').
  - `description`: VARCHAR(120) (Descrição humanizada do tipo).
  - `enabled`: BOOLEAN (Indica se o tipo de mídia está ativo para novos cadastros).
- **MediaItem (Entidade de Domínio)**:
  - `id`: UUID (Chave primária).
  - `mediaTypeCode`: VARCHAR(30) (FK para MediaTypeCatalog, Obrigatório).
  - `url`: VARCHAR(2048) (Obrigatório, Único, esquemas HTTP/HTTPS).
  - `cachedFileName`: VARCHAR(255) (Identificador lógico do recurso de fallback/cache — resolução de backend de armazenamento, ex.: S3/MinIO, fora de escopo desta feature).
  - `fallbackApplied`: BOOLEAN (Obrigatório, Padrão: FALSE).
  - `createdAt`: TIMESTAMPTZ.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% das URLs remotas inacessíveis são salvas com sucesso marcadas com `fallback_applied = true`, sem gerar erros HTTP 500 no cliente.
- **SC-002**: 100% das tentativas de cadastro de URLs duplicadas ou formatos de URL inválidos são rejeitadas com erros amigáveis RFC 7807.
- **SC-003**: 100% das exclusões de `MediaItem` vinculado a evento são concluídas com sucesso (204) e resultam em `event.media_item_id = NULL` para os eventos afetados.

### Performance and Resilience Outcomes *(mandatory)*

- **PR-001**: Taxa de erros não tratados do servidor (5xx) em steady-state < 0,1%.

> Metas específicas de latência (timeout de verificação de URL, P95 de escrita) foram movidas para Assumptions — não possuem lastro em `arquitetura-solucao.md` e são propostas desta spec, não requisito herdado.

## Assumptions

- O cadastro e validação de mídias é executado no microsserviço `microservice-catalog` utilizando o schema `catalog.media_item` e `catalog.media_type_catalog` no PostgreSQL (`catalog_db`).
- O timeout de 250 ms para verificação síncrona da URL remota e a meta de P95 <= 300 ms para operações de escrita são propostos por esta spec, não confirmados em `arquitetura-solucao.md` — sujeitos a validação em teste de carga antes de virarem SLO.
- O arquivo de fallback padrão (`not_available.jpg`) é um asset estático servido pelo próprio serviço ou por um bucket de object storage dedicado — a resolução definitiva do backend de armazenamento (substituto do `tmpDir` do legado, conforme `arquitetura-solucao.md` seção 16) é tratada como decisão de infraestrutura fora do escopo funcional desta feature.
- Inclusão de novos `media_type_code` no catálogo de tipos é operação de migração de banco/deploy, não uma capacidade self-service exposta por API nesta feature.
- O cache de itens de mídia individuais em Redis (FR-007) usa contrato de chave proposto por esta spec, não herdado da arquitetura de referência.
- Autenticação e autorização providas pelo Keycloak JWT exigindo a role `ROLE_ADMIN` para escrita.

## Rastreabilidade

| Item desta spec | Origem | Observação |
|---|---|---|
| FR-001, FR-002 | `microservice-catalog_spec.md` US-CAT-12, RN37 | CRUD + unicidade/esquema de URL |
| FR-003 | RN35, CA-CAT-03-MED | Corrigido para explicitar não-bloqueio de thread (ver `arquitetura-solucao.md`, Diagnóstico item 4) |
| FR-004 | `[ALTERA RN34]`, US-CAT-14 | Catálogo extensível; escopo de API restrito ao consumo |
| FR-005 | `arquitetura-solucao.md` seção 8 | RFC 7807 |
| FR-007 | Extensão local (não herdada) | Chave de cache não definida em `arquitetura-solucao.md` seção 10 |
| FR-008 | DDL `event.media_item_id ... ON DELETE SET NULL`, RN05 | Corrige contradição da versão anterior desta spec (US3/AC2) |
| `cachedFileName` como identificador lógico | `arquitetura-solucao.md` seção 16 (object storage substitui `tmpDir`) | Backend de armazenamento não resolvido em nenhum documento-fonte |